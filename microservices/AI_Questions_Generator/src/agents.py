from __future__ import annotations

import json
from dataclasses import dataclass

from openai import OpenAI

from .i18n import LANGUAGE_NAMES
from .models import EvaluationResult, GenerationRequest, MathProblem


# ---------------------------------------------------------------------------
# System prompts
# ---------------------------------------------------------------------------

GENERATOR_SYSTEM_PROMPT = """\
You are a world-class educational question designer for children and students of all ages.

Your task is to create ONE engaging, imaginative, and curriculum-aligned question.

RULES:
1. TOPIC — The question must genuinely test the given subject.
   Examples: "fractions" → student must work with fractions; "history" → question involves real facts or reasoning.
   The topic defines WHAT the student is learning — never skip it.

2. THEME — Weave the creative theme naturally into the narrative/story of the question.
   The theme is the world/setting, not the subject itself.
   Make it vivid and fun — names, places, objects should all fit the theme.

3. DIFFICULTY (1–10) — Scale complexity accordingly:
   1–2  → kindergarten/early primary (counting, basic shapes, simple facts)
   3–4  → primary school (double-digit arithmetic, introductory concepts)
   5–6  → middle school (fractions, percentages, multi-step reasoning)
   7–8  → late middle / early high school (algebra, ratios, deeper analysis)
   9–10 → advanced high school (complex equations, proofs, advanced topics)
   Use the student's AGE as a secondary guide, but DIFFICULTY takes priority.

4. PERSONALISATION — Mention the student by NAME somewhere in the question to make it feel personal.

5. ACCURACY — The correct_answer MUST be exactly right. \
Double-check all arithmetic, logic, and facts before responding. \
Never guess.

6. SOLUTION — step_by_step_solution must walk the student through the reasoning clearly. \
Each step = one sentence. Minimum 2 steps, maximum 6 steps. \
Steps must be logically ordered and lead to the correct answer.

7. LANGUAGE — Write ENTIRELY in the requested output language. Do not mix languages under any circumstances.

8. SAFETY — Never include violence, weapons, drugs, alcohol, adult content, or anything \
inappropriate for children.

OUTPUT — Return ONLY a valid JSON object with exactly these three keys, nothing else:
{
  "question_text": "...",
  "correct_answer": "...",
  "step_by_step_solution": ["Step 1 ...", "Step 2 ...", "Step 3 ..."]
}
"""

EVALUATOR_SYSTEM_PROMPT = """\
You are a precise answer-verification agent for an educational platform.

Given a question, its proposed correct answer, and a step-by-step solution, \
determine whether the answer is correct and the solution is sound.

Verification approach:
- MATH questions: verify every arithmetic/algebraic step; confirm the final answer matches.
- NON-MATH questions (history, science, geography, etc.): check factual accuracy and logical soundness.
- Scrutinise every step of the provided solution for errors or logical jumps.
- If an answer is approximately correct but has a minor rounding difference, mark it correct \
  and note it in the explanation.

Return ONLY a valid JSON object:
{
  "is_correct": true,
  "explanation": "One-sentence verdict explaining your reasoning."
}
"""


# ---------------------------------------------------------------------------
# Agent
# ---------------------------------------------------------------------------


@dataclass(slots=True)
class QuestionAgent:
    """
    LLM-powered agent that:
    1. Generates a themed, personalised educational question (``generate``).
    2. Independently verifies the answer correctness (``evaluate``).
    """

    client: OpenAI
    model: str

    # ------------------------------------------------------------------
    # Public interface
    # ------------------------------------------------------------------

    def generate(self, req: GenerationRequest) -> MathProblem:
        """Call the LLM to produce a rich, themed question from the request."""
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": GENERATOR_SYSTEM_PROMPT},
                {"role": "user", "content": self._build_generation_prompt(req)},
            ],
            response_format={"type": "json_object"},
            temperature=0.85,  # creative but not chaotic
        )

        raw: dict = json.loads(response.choices[0].message.content)

        return MathProblem(
            question_text=raw["question_text"],
            correct_answer=str(raw["correct_answer"]),
            step_by_step_solution=raw["step_by_step_solution"],
            difficulty_level=req.difficulty,
            language=req.language,
        )

    def evaluate(self, problem: MathProblem) -> EvaluationResult:
        """
        Run a separate LLM call to verify the generated answer is correct.

        Using a second, zero-temperature call rather than asking the model to
        self-verify in the same turn — this catches hallucinations the generator
        may have made while being creative.
        """
        steps_text = "\n".join(
            f"  {i + 1}. {step}" for i, step in enumerate(problem.step_by_step_solution)
        )
        user_prompt = (
            f"Question: {problem.question_text}\n\n"
            f"Proposed correct answer: {problem.correct_answer}\n\n"
            f"Step-by-step solution:\n{steps_text}"
        )

        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": EVALUATOR_SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            response_format={"type": "json_object"},
            temperature=0.0,  # deterministic: we want the same verdict every time
        )

        raw: dict = json.loads(response.choices[0].message.content)
        is_valid = bool(raw.get("is_correct", False))
        explanation = str(raw.get("explanation", "No explanation provided."))

        return EvaluationResult(
            is_valid=is_valid,
            calculated_answer=problem.correct_answer,
            generated_answer=problem.correct_answer,
            feedback=explanation,
        )

    # ------------------------------------------------------------------
    # Private helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _build_generation_prompt(req: GenerationRequest) -> str:
        """
        Construct the user-turn prompt that feeds all request fields to the
        generator.  Extra ``user_info`` fields (beyond name/age) are appended
        automatically so the model can use them for personalisation.
        """
        lines = [
            f"Topic: {req.topic}",
            f"Theme: {req.theme}",
            f"Difficulty: {req.difficulty}/10",
            f"Student name: {req.user_info.name}",
            f"Student age: {req.user_info.age} years old",
            f"Output language: {LANGUAGE_NAMES.get(req.language, req.language)}",
        ]

        # Forward any extra student fields (e.g. grade, learning_style) to the LLM
        extra = req.user_info.model_extra or {}
        if extra:
            extra_parts = ", ".join(f"{k}: {v}" for k, v in extra.items())
            lines.append(f"Additional student context: {extra_parts}")

        return "\n".join(lines)
