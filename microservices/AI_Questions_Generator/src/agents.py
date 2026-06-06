from __future__ import annotations

import json
import re
from dataclasses import dataclass
from typing import Any, cast

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

FEW_SHOT_EXAMPLES = [
    {
        "role": "user",
        "content": "Topic: Addition, Difficulty: 1, Context: pony stickers, Language: en",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "A child has 2 pony stickers and gets 3 more pony stickers. How many pony stickers are there now?",
                "correct_answer": "5",
                "step_by_step_solution": [
                    "Start with 2 pony stickers.",
                    "Add 3 more: 2 + 3 = 5.",
                    "The answer is 5.",
                ],
                "difficulty_level": 1,
                "language": "en",
                "math_expression": "2 + 3",
            },
            ensure_ascii=False,
        ),
    },
    {
        "role": "user",
        "content": "Topic: Geometry, Difficulty: 2, Context: playground, Language: he",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "לריבוע של פינת משחק יש אורך צלע 4. מה השטח שלו?",
                "correct_answer": "16",
                "step_by_step_solution": [
                    "נשתמש בנוסחת השטח של ריבוע: צלע × צלע.",
                    "נציב: 4 × 4 = 16.",
                    "השטח הוא 16.",
                ],
                "difficulty_level": 2,
                "language": "he",
                "math_expression": "4 * 4",
            },
            ensure_ascii=False,
        ),
    },
    {
        "role": "user",
        "content": "Topic: Addition, Difficulty: 2, Context: toy rockets, Language: ru",
    },
    {
        "role": "assistant",
        "content": json.dumps(
            {
                "question_text": "У ребёнка было 10 игрушечных ракет, и ему подарили ещё 6. Сколько ракет стало теперь?",
                "correct_answer": "16",
                "step_by_step_solution": [
                    "Начинаем с 10 игрушечных ракет.",
                    "Добавляем 6: 10 + 6 = 16.",
                    "Ответ: 16.",
                ],
                "difficulty_level": 2,
                "language": "ru",
                "math_expression": "10 + 6",
            },
            ensure_ascii=False,
        ),
    },
]


@dataclass(slots=True)
class MathAgents:
    client: OpenAI | None
    model: str

    def route(self, req: GenerationRequest) -> RouterDecision:
        topic_lower = req.topic.lower()
        route = MathRoute.geometry if any(word in topic_lower for word in ["geometry", "shape", "area", "perimeter"]) else MathRoute.arithmetic
        rationale = GEOMETRY_ROUTE_HINTS[req.language] if route == MathRoute.geometry else ARITHMETIC_ROUTE_HINTS[req.language]
        return RouterDecision(route=route, rationale=rationale)

    def generate(self, req: GenerationRequest, route: MathRoute) -> MathProblem:
        if self.client is None:
            return self._generate_fallback(req, route)

        last_error: str | None = None
        for _ in range(3):
            try:
                problem = self._generate_with_llm(req, route, last_error)
                evaluation = self.evaluate(problem)
                if evaluation.is_valid:
                    return problem
                last_error = evaluation.feedback
            except Exception as exc:
                last_error = str(exc)

        return self._generate_fallback(req, route)

    def _generate_with_llm(self, req: GenerationRequest, route: MathRoute, last_error: str | None = None) -> MathProblem:
        messages: list[dict[str, Any]] = [{"role": "system", "content": GENERATOR_SYSTEM_PROMPT}]
        messages.extend(FEW_SHOT_EXAMPLES)
        messages.append({"role": "user", "content": self._build_user_prompt(req, route, last_error)})

        completion = self.client.beta.chat.completions.parse(  # type: ignore[union-attr]
            model=self.model,
            messages=cast(Any, messages),
            response_format=MathProblem,
            temperature=0.4,
        )

        problem = completion.choices[0].message.parsed
        if problem is None:
            raise ValueError("The model returned an empty structured response.")

        problem.language = req.language
        problem.difficulty_level = req.difficulty
        problem.route = route
        return problem

    def _build_user_prompt(self, req: GenerationRequest, route: MathRoute, last_error: str | None = None) -> str:
        context_line = req.context.strip() if req.context.strip() else "none"
        base_prompt = (
            f"Topic: {req.topic}\n"
            f"Difficulty: {req.difficulty}\n"
            f"Context: {context_line}\n"
            f"Language: {req.language.value}\n"
            f"Route: {route.value}\n\n"
            "Create a child-safe, engaging word problem that uses the context naturally. "
            "Make the story feel like something the child likes. If the context mentions a character, toy, show, animal, game, or hobby, weave it into the story naturally and clearly. "
            "Write at least 18 words in question_text, and make it sound like a mini story (setting + characters + action). "
            "Return JSON only with question_text, correct_answer, step_by_step_solution, difficulty_level, language, and math_expression. "
            "math_expression must be a simple arithmetic expression that matches the answer."
        )

        if last_error:
            base_prompt += f"\nPrevious attempt error: {last_error}. Improve the story and ensure the answer matches the math_expression exactly."

        return base_prompt

    def _generate_fallback(self, req: GenerationRequest, route: MathRoute) -> MathProblem:
        math_expression = extract_math_expression(req.topic, req.context, req.difficulty)
        calculated = calculate_expression(math_expression)
        correct_answer = str(int(calculated)) if calculated.is_integer() else str(calculated)

        context = req.context.strip()
        if route == MathRoute.geometry:
            side = math_expression.split(" * ")[0]
            if context:
                question_text = {
                    "en": f"In a fun {context} adventure park, a square play zone has side length {side} meters. What is the area of this play zone?",
                    "he": f"בפארק הרפתקאות של {context}, אזור המשחק הוא ריבוע באורך צלע {side}. מה השטח של אזור המשחק?",
                    "ru": f"В парке приключений по теме {context} есть квадратная игровая зона со стороной {side}. Какова площадь этой зоны?",
                }[req.language.value]
            else:
                question_text = QUESTION_TEMPLATES[req.language]["geometry"].format(side=side)
            steps = [step.format(expression=math_expression, answer=correct_answer, first_number=side) for step in STEP_TEMPLATES[req.language]["geometry"]]
        else:
            if context:
                first, second = self._extract_numbers(math_expression)
                unit = self._derive_context_unit(context, req.language.value)
                setting = self._derive_setting(context, req.language.value)
                question_text = {
                    "en": (
                        f"On the beautiful {setting}, there were {first} happy {unit}. "
                        f"Then a group of {second} more {unit} joined them after seeing the fun place. "
                        f"How many {unit} are on the {setting} now?"
                    ),
                    "he": (
                        f"ב-{setting} היפה היו {first} {unit} שמחים. "
                        f"אחר כך הצטרפה קבוצה של עוד {second} {unit}. "
                        f"כמה {unit} יש עכשיו ב-{setting}?"
                    ),
                    "ru": (
                        f"На прекрасном месте под названием {setting} было {first} счастливых {unit}. "
                        f"Потом к ним присоединились ещё {second} {unit}. "
                        f"Сколько {unit} стало теперь в {setting}?"
                    ),
                }[req.language.value]
                steps = {
                    "en": [
                        f"At the beginning, there are {first} {unit} on the {setting}.",
                        f"Then {second} more {unit} join, so we add: {first} + {second}.",
                        f"{first} + {second} = {correct_answer}, so there are {correct_answer} {unit} in total.",
                    ],
                    "he": [
                        f"בהתחלה יש {first} {unit} ב-{setting}.",
                        f"מצטרפים עוד {second} {unit}, לכן מחשבים: {first} + {second}.",
                        f"{first} + {second} = {correct_answer}, ולכן יש בסך הכל {correct_answer} {unit}.",
                    ],
                    "ru": [
                        f"Сначала на {setting} было {first} {unit}.",
                        f"Потом присоединились ещё {second}, значит считаем: {first} + {second}.",
                        f"{first} + {second} = {correct_answer}, всего стало {correct_answer} {unit}.",
                    ],
                }[req.language.value]
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

    def _extract_numbers(self, expression: str) -> tuple[str, str]:
        numbers = re.findall(r"\d+(?:\.\d+)?", expression)
        if len(numbers) >= 2:
            return numbers[0], numbers[1]
        if len(numbers) == 1:
            return numbers[0], "1"
        return "5", "3"

    def _derive_setting(self, context: str, language: str) -> str:
        value = context.strip()
        if not value:
            return {
                "en": "magic island",
                "he": "אי קסום",
                "ru": "волшебный остров",
            }[language]
        if language == "en" and "island" not in value.lower():
            return f"{value} island"
        return value

    def _derive_context_unit(self, context: str, language: str) -> str:
        value = context.strip().lower()
        if language == "en":
            if any(word in value for word in ["pony", "ponies", "poney"]):
                return "ponies"
            if any(word in value for word in ["cat", "cats"]):
                return "cats"
            if any(word in value for word in ["dog", "dogs"]):
                return "dogs"
            return "friends"
        if language == "he":
            if "סוס" in value or "פוני" in value:
                return "פונים"
            return "חברים"
        if any(word in value for word in ["пони", "лошад"]):
            return "пони"
        return "друзей"

    def evaluate(self, problem: MathProblem) -> EvaluationResult:
        if not problem.math_expression:
            return EvaluationResult(
                is_valid=False,
                calculated_answer="",
                generated_answer=problem.correct_answer,
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

