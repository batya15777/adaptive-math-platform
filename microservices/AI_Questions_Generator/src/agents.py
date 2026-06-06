from __future__ import annotations

import json
from dataclasses import dataclass

from openai import OpenAI

from .i18n import ARITHMETIC_ROUTE_HINTS, GEOMETRY_ROUTE_HINTS, QUESTION_TEMPLATES, STEP_TEMPLATES
from .models import EvaluationResult, GenerationRequest, MathProblem, MathRoute, RouterDecision
from .tools import calculate_expression, extract_math_expression

ROUTER_SYSTEM_PROMPT = (
    "You are a routing agent for a math question generator. "
    "Route arithmetic topics to 'arithmetic' and geometry topics to 'geometry'. "
    "Return only structured JSON."
)

GENERATOR_SYSTEM_PROMPT = (
    "You are a math tutor for children. Always output the required JSON format. "
    "Do not include violence, alcohol, weapons, or adult themes in word problems. "
    "Use concise, age-appropriate language and provide a clear step-by-step solution."
)

import json

FEW_SHOT_EXAMPLES = [
    # Example 1: Basic Addition (Level 1) - Kindergarten/Early Elementary
    {
        "role": "user",
        "content": "Topic: Addition, Difficulty: 1, Context: ''",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "מה זה 2 + 3?",
                "correct_answer": "5",
                "step_by_step_solution": [
                    "נתחיל מהמספר 2.",
                    "נספור עוד 3 צעדים קדימה: 3, 4, 5.",
                    "הגענו למספר 5, וזו התשובה!"
                ],
                "difficulty_level": 1,
            },
            ensure_ascii=False
        ),
    },

    # Example 2: Subtraction with Decimal Transition (Level 2) - Elementary School
    {
        "role": "user",
        "content": "Topic: Subtraction, Difficulty: 2, Context: ''",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "כמה זה 72 - 29?",
                "correct_answer": "43",
                "step_by_step_solution": [
                    "נכתוב את התרגיל במאונך או נפרק אותו לעשרות ויחידות.",
                    "נפחית תחילה את העשרות: 72 פחות 20 שווה 52.",
                    "עכשיו נשאר להפחית את 9 היחידות: 52 פחות 9.",
                    "נרד 2 צעדים אל ה-50, ואז עוד 7 צעדים אל ה-43.",
                    "התשובה הסופית היא 43."
                ],
                "difficulty_level": 2,
            },
            ensure_ascii=False
        ),
    },

    # Example 3: Dynamic Word Problem (Level 3) - Context Driven
    {
        "role": "user",
        "content": "Topic: Word Problem, Difficulty: 3, Context: 'מדף וספרים'",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "על המדף היו 40 ספרים. המורה אספה 18 ספרים מהמדף. כמה ספרים נשארו על המדף כעת?",
                "correct_answer": "22",
                "step_by_step_solution": [
                    "נבין את הסיפור: התחלנו עם 40 ספרים, והורידו מהם 18 ספרים.",
                    "זוהי פעולת חיסור: 18 - 40.",
                    "נחסר תחילה 10: 30 = 10 - 40.",
                    "כעת נחסר את 8 היחידות הנותרות: 22 = 8 - 30.",
                    "על המדף נשארו 22 ספרים."
                ],
                "difficulty_level": 3,
            },
            ensure_ascii=False
        ),
    },

    # Example 4: Number Completion (Level 4) - Missing Variable
    {
        "role": "user",
        "content": "Topic: Number Completion, Difficulty: 4, Context: ''",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "מצאו את המספר החסר: 45 = ? + 20",
                "correct_answer": "25",
                "step_by_step_solution": [
                    "נתון לנו שסכום שני מספרים הוא 45, ואחד מהם הוא 20.",
                    "כדי למצוא את המספר השני (הנעלם), נבצע פעולה הפוכה - חיסור.",
                    "נחסר את המספר הידוע מהסכום הכולל: 20 - 45.",
                    "45 פחות 20 שווה ל-25.",
                    "המספר החסר הוא 25."
                ],
                "difficulty_level": 4,
            },
            ensure_ascii=False
        ),
    },

    # Example 5: High-Level Math (Level 5) - Statistics/Algebra for Academic/High-School
    {
        "role": "user",
        "content": "Topic: Statistics, Difficulty: 5, Context: ''",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "נתון מדגם של 5 ציונים: 70, 80, 85, 90, 100. מהו הממוצע (Mean) של ציונים אלו?",
                "correct_answer": "85",
                "step_by_step_solution": [
                    "כדי למצוא ממוצע של קבוצת נתונים, עלינו לחבר את כל הערכים יחד ולחלק במספר האיברים.",
                    "שלב 1: נחשב את סכום הציונים: 425 = 100 + 90 + 85 + 80 + 70.",
                    "שלב 2: נספור כמה ציונים יש במדגם. ישנם 5 ציונים.",
                    "שלב 3: נחלק את הסכום במספר האיברים: 425 חלקי 5.",
                    "425 / 5 = 85. הממוצע של המדגם הוא 85."
                ],
                "difficulty_level": 5,
            },
            ensure_ascii=False
        ),
    }
]


@dataclass(slots=True)
class MathAgents:
    client: OpenAI
    model: str

    def route(self, req: GenerationRequest) -> RouterDecision:
        topic_lower = req.topic.lower()
        route = MathRoute.geometry if any(word in topic_lower for word in ["geometry", "shape", "area", "perimeter"]) else MathRoute.arithmetic
        rationale = GEOMETRY_ROUTE_HINTS[req.language] if route == MathRoute.geometry else ARITHMETIC_ROUTE_HINTS[req.language]
        return RouterDecision(route=route, rationale=rationale)

    def generate(self, req: GenerationRequest, route: MathRoute) -> MathProblem:
        math_expression = extract_math_expression(req.topic, req.context, req.difficulty)
        calculated = calculate_expression(math_expression)
        correct_answer = str(int(calculated)) if calculated.is_integer() else str(calculated)

        if route == MathRoute.geometry:
            side = math_expression.split(" * ")[0]
            question_text = QUESTION_TEMPLATES[req.language]["geometry"].format(side=side)
            steps = [step.format(expression=math_expression, answer=correct_answer, first_number=side) for step in STEP_TEMPLATES[req.language]["geometry"]]
        else:
            question_text = QUESTION_TEMPLATES[req.language]["arithmetic"].format(expression=math_expression)
            first_number = math_expression.split(" ")[0]
            steps = [step.format(expression=math_expression, answer=correct_answer, first_number=first_number) for step in STEP_TEMPLATES[req.language]["arithmetic"]]

        response = MathProblem(
            question_text=question_text,
            correct_answer=correct_answer,
            step_by_step_solution=steps,
            difficulty_level=req.difficulty,
            language=req.language,
            math_expression=math_expression,
            route=route,
        )
        return response

    def evaluate(self, problem: MathProblem) -> EvaluationResult:
        if not problem.math_expression:
            answer_str = str(problem.correct_answer)  # type: ignore
            return EvaluationResult(
                is_valid=False,
                calculated_answer="",
                generated_answer=answer_str,
                feedback="evaluation skipped: no math_expression provided",
            )

        calculated = calculate_expression(problem.math_expression)
        generated = float(problem.correct_answer)
        is_valid = abs(calculated - generated) < 1e-9
        return EvaluationResult(
            is_valid=is_valid,
            calculated_answer=str(int(calculated)) if calculated.is_integer() else str(calculated),
            generated_answer=str(problem.correct_answer),
            feedback="Answer verified successfully." if is_valid else "Generated answer did not match calculator verification.",
        )

