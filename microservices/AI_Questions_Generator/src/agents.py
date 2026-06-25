from __future__ import annotations

import json
import logging
from dataclasses import dataclass

from openai import OpenAI

from .i18n import LANGUAGE_NAMES
from .models import EvaluationResult, GenerationRequest, MathProblem

log = logging.getLogger("ai_questions.agent")


# ---------------------------------------------------------------------------
# System prompts
# ---------------------------------------------------------------------------

GENERATOR_SYSTEM_PROMPT = """\
You are a world-class educational question designer for children and students of all ages.

Your task is to create ONE engaging, imaginative, and curriculum-aligned question.

RULES:

1. TOPIC — The question must genuinely test the given subject.
   Examples: "fractions" → student must work with fractions; "history" → question involves real facts.
   The topic defines WHAT the student is learning — never skip it.

2. THEME — Weave the creative theme naturally into the narrative/story of the question.
   The theme is the world/setting, not the subject itself. Make it vivid and fun.

3. DIFFICULTY (1–10) — Scale complexity accordingly:
   1–2  → kindergarten / early primary (counting, basic shapes, simple facts)
   3–4  → primary school (double-digit arithmetic, introductory concepts)
   5–6  → middle school (fractions, percentages, multi-step reasoning)
   7–8  → late middle / early high school (algebra, ratios, deeper analysis)
   9–10 → advanced high school (complex equations, proofs, advanced topics)

4. PERSONALISATION — Mention the student using the exact placeholder word NAME (always
   written in English, uppercase, regardless of the output language). Do NOT substitute a
   real name — write the literal string NAME so the question is reusable for any student.

5. CORRECT ANSWER — Must be a minimal, directly-comparable value:
   • Numbers: digits only — write "26" not "26 apples"; fractions as "3/4"; decimals as "3.5"
   • Text answers: shortest definitive phrase — "1066", "Julius Caesar", "photosynthesis"
   • No prose, no explanation, no leading/trailing spaces, no units unless the unit IS the full answer use European Union's standard system of measurement
   The frontend compares user input directly against this string — keep it clean and predictable.

6. MULTIPLE CHOICE (only when the mode field says "multiple_choice"):
   • Add an "options" array of exactly 4 strings
   • Exactly ONE option must be identical (character-for-character) to correct_answer
   • The other 3 must be plausible but wrong distractors in the same format
   • Shuffle so the correct option is not always in the same position
   • All 4 options must be the same type (all numbers, all names, all short phrases, etc.)

7. SOLUTION — step_by_step_solution walks the student through the reasoning.
   Each step = one sentence. Minimum 2, maximum 6 steps. Logically ordered. Leads to correct_answer.

8. LANGUAGE — Write ENTIRELY in the requested output language. Do not mix languages.

9. SAFETY — Never include violence, weapons, drugs, alcohol, adult content, or anything
   inappropriate for children.

10. VARIETY — Never default to the same "safe" or round numbers across questions.
    • Percentages: do NOT default to 10 %, 25 %, 50 %, or 100 %. Choose realistic but
      varied values such as 15 %, 37 %, 62 %, 84 %.
    • Prices, distances, quantities, counts: mix single-digit, double-digit, and
      multi-digit numbers freely.
    • Each question must feel numerically distinct — avoid re-using the same values
      you just used in the previous question.

OUTPUT — Return ONLY a valid JSON object.

Open-answer mode:
{
  "question_text": "...",
  "correct_answer": "...",
  "step_by_step_solution": ["Step 1 ...", "Step 2 ...", "Step 3 ..."]
}

Multiple-choice mode (add the "options" key):
{
  "question_text": "...",
  "correct_answer": "...",
  "options": ["choice A", "choice B", "choice C", "choice D"],
  "step_by_step_solution": ["Step 1 ...", "Step 2 ...", "Step 3 ..."]
}
"""

EVALUATOR_SYSTEM_PROMPT = """\
You are a precise answer-verification agent for an educational platform.

Given a question, its proposed correct answer, and a step-by-step solution, determine whether
the answer is correct and the solution is sound.

Verification approach:
- MATH: verify every arithmetic/algebraic step; confirm the final answer matches.
- NON-MATH (history, science, geography, …): check factual accuracy and logical soundness.
- Scrutinise every solution step for errors or unjustified leaps.
- If the answer is approximately correct with only a minor rounding difference, mark it correct and note it.

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
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": GENERATOR_SYSTEM_PROMPT},
                    {"role": "user", "content": self._build_generation_prompt(req)},
                ],
                response_format={"type": "json_object"},
                temperature=0.85,
            )
        except Exception:
            log.exception("    OpenAI generate() call FAILED (model=%s)", self.model)
            raise

        raw: dict = json.loads(response.choices[0].message.content)

        # --- Parse and validate multiple-choice options if requested ---
        options: list[str] | None = None
        if req.multiple_choice:
            options = raw.get("options")
            if not isinstance(options, list) or len(options) != 4:
                raise ValueError(
                    "Model did not return exactly 4 options for a multiple-choice question. "
                    "Will retry."
                )
            correct = str(raw.get("correct_answer", ""))
            if correct not in options:
                raise ValueError(
                    f"correct_answer '{correct}' is not present in the options list. "
                    "Will retry."
                )

        return MathProblem(
            question_text=raw["question_text"],
            correct_answer=str(raw["correct_answer"]).strip(),
            options=options,
            step_by_step_solution=raw["step_by_step_solution"],
            difficulty_level=req.difficulty,
            language=req.language,
        )

    def evaluate(self, problem: MathProblem) -> EvaluationResult:
        """
        Run a separate zero-temperature LLM call to verify the generated answer.

        Using a second, independent call rather than self-verification in the same
        turn — this catches hallucinations the creative generator may have introduced.
        """
        steps_text = "\n".join(
            f"  {i + 1}. {step}" for i, step in enumerate(problem.step_by_step_solution)
        )
        user_prompt = (
            f"Question: {problem.question_text}\n\n"
            f"Proposed correct answer: {problem.correct_answer}\n\n"
            f"Step-by-step solution:\n{steps_text}"
        )

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": EVALUATOR_SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt},
                ],
                response_format={"type": "json_object"},
                temperature=0.0,
            )
        except Exception:
            log.exception("    OpenAI evaluate() call FAILED (model=%s)", self.model)
            raise

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
        """Build the user-turn prompt that feeds all request fields to the generator."""
        mode = "multiple_choice" if req.multiple_choice else "open_answer"

        lines = [
            f"Topic: {req.topic}",
            f"Theme: {req.theme}",
            f"Difficulty: {req.difficulty}/10",
        ]
        if req.difficulty_band:
            lines.append(
                f"Difficulty band: {req.difficulty_band} "
                "(supplementary hint — Difficulty above is authoritative)"
            )
        language_name = LANGUAGE_NAMES.get(req.language, req.language)
        lines += [
            f"Output language: {language_name}",
            f"Mode: {mode}",
        ]

        adaptive_block = QuestionAgent._build_adaptive_block(req)
        if adaptive_block:
            lines.append(adaptive_block)

        # Emphatic, final language directive — the single line above is easy for the model to
        # overlook in a long prompt, which leads to English answers slipping through.
        lines.append(
            f"CRITICAL: Write question_text and EVERY step in step_by_step_solution entirely "
            f"in {language_name}. The ONLY exception is the literal placeholder word NAME, which "
            f"stays in English. Do NOT answer in English unless English is the requested language."
        )

        return "\n".join(lines)

    @staticmethod
    def _build_adaptive_block(req: GenerationRequest) -> str:
        """
        Build the adaptive-learning section from the cluster-derived learning profile.
        The model is told to USE these hints silently (never to mention them to the student).
        """
        lp = req.learning_profile
        if lp is None or not (lp.focus_skill or lp.cluster_label or lp.mastery):
            return ""

        parts = ["ADAPTIVE LEARNING CONTEXT (personalise using this; never mention it to the student):"]
        if lp.cluster_label:
            parts.append(f"- Learning cohort: {lp.cluster_label}.")
        if lp.mastery:
            parts.append(f"- Current mastery: {lp.mastery} — calibrate the challenge accordingly.")
        if lp.focus_skill:
            parts.append(
                f"- The student needs practice with: {lp.focus_skill}. "
                "When it fits the topic naturally, weave a little of this skill into the question."
            )
        return "\n".join(parts)
