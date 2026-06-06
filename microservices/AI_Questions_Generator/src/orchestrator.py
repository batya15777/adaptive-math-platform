from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from openai import OpenAI

from .agents import QuestionAgent
from .guardrails import ModerationGuardrail, check_output_guardrails
from .i18n import EMPTY_RESPONSE_MESSAGES
from .models import GenerationRequest, MathProblem
from .settings import Settings

_SAFETY_SENTINELS = frozenset(
    {
        "System Error",
        "I cannot process this request due to safety protocols",
    }
)


@dataclass(slots=True)
class QuestionGenerationOrchestrator:
    settings: Settings
    client: OpenAI | None
    _guardrail: Any = None
    _agent: Any = None

    def __post_init__(self) -> None:
        if self.client is None:
            return
        self._guardrail = ModerationGuardrail(self.client)
        self._agent = QuestionAgent(client=self.client, model=self.settings.openai_model)

    def generate(self, req: GenerationRequest) -> MathProblem:
        if self.client is None or self._guardrail is None or self._agent is None:
            raise RuntimeError(
                "OPENAI_API_KEY not found. "
                "Set it in the container environment or pass --env-file .env when running Docker."
            )

        # --- Input guardrails: check topic + theme + student name ---
        guard_text = f"{req.topic} {req.theme} {req.user_info.name}"
        blocked = self._guardrail.check(guard_text, req.language)
        if blocked:
            raise ValueError(blocked)

        # --- Generate + verify (with one automatic retry) ---
        problem = self._generate_and_verify(req)

        # --- Output guardrails: final safety pass on the question text ---
        safe_text = check_output_guardrails(problem.question_text)
        if any(safe_text.startswith(sentinel) for sentinel in _SAFETY_SENTINELS):
            raise ValueError(EMPTY_RESPONSE_MESSAGES[req.language])
        problem.question_text = safe_text

        return problem

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _generate_and_verify(self, req: GenerationRequest) -> MathProblem:
        """
        Generate a question and verify its answer.
        Retries once automatically if:
        - generate() raises ValueError (e.g. malformed multiple-choice options), or
        - evaluate() rejects the answer.
        Raises ValueError to the caller if both attempts fail.
        """
        last_error: str = ""
        for attempt in range(2):
            try:
                problem = self._agent.generate(req)
            except ValueError as exc:
                # Malformed generation (e.g. wrong option count) — retry once
                last_error = str(exc)
                if attempt == 0:
                    continue
                raise ValueError(
                    f"Question generation failed after 2 attempts. Last error: {last_error}"
                ) from exc

            evaluation = self._agent.evaluate(problem)
            if evaluation.is_valid:
                return problem

            last_error = evaluation.feedback
            if attempt == 0:
                continue

            raise ValueError(
                f"Answer verification failed after 2 attempts. "
                f"Last evaluator feedback: {last_error}"
            )

        # Unreachable, but satisfies type checkers
        raise RuntimeError("Unexpected state in _generate_and_verify")
