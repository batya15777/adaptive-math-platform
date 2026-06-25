from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from openai import OpenAI

from .agents import QuestionAgent
from .guardrails import ModerationGuardrail, check_output_guardrails
from .i18n import EMPTY_RESPONSE_MESSAGES
from .language_check import language_mismatch
from .models import GenerationRequest, MathProblem
from .settings import Settings

log = logging.getLogger("ai_questions.orchestrator")

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

        # --- Input guardrails: check topic + theme ---
        log.info("[1/4] Input guardrails…")
        guard_text = f"{req.topic} {req.theme}"
        blocked = self._guardrail.check(guard_text, req.language)
        if blocked:
            log.warning("[1/4] Input BLOCKED by guardrails: %s", blocked)
            raise ValueError(blocked)
        log.info("[1/4] Input guardrails passed.")

        # --- Generate + verify (with one automatic retry) ---
        log.info("[2/4] Generating + verifying question…")
        problem = self._generate_and_verify(req)
        log.info("[3/4] Generation + verification done.")

        # --- Output guardrails: final safety pass on the question text ---
        log.info("[4/4] Output guardrails…")
        safe_text = check_output_guardrails(problem.question_text)
        if any(safe_text.startswith(sentinel) for sentinel in _SAFETY_SENTINELS):
            log.warning("[4/4] Output BLOCKED by guardrails.")
            raise ValueError(EMPTY_RESPONSE_MESSAGES[req.language])
        problem.question_text = safe_text
        log.info("[4/4] Output guardrails passed.")

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
            log.info("  generate(): attempt %d/2 — calling OpenAI…", attempt + 1)
            try:
                problem = self._agent.generate(req)
            except ValueError as exc:
                # Malformed generation (e.g. wrong option count) — retry once
                last_error = str(exc)
                log.warning("  generate(): attempt %d rejected: %s", attempt + 1, last_error)
                if attempt == 0:
                    continue
                raise ValueError(
                    f"Question generation failed after 2 attempts. Last error: {last_error}"
                ) from exc

            # The model sometimes ignores the requested language and answers in English.
            # Treat that like a failed verification: retry once, then fail loudly rather
            # than caching a wrong-language question.
            mismatch = language_mismatch(problem.question_text, req.language)
            if mismatch:
                last_error = mismatch
                log.warning("  generate(): attempt %d wrong language: %s", attempt + 1, last_error)
                if attempt == 0:
                    continue
                raise ValueError(
                    f"Question generation produced the wrong language after 2 attempts. "
                    f"Last error: {last_error}"
                )

            log.info("  evaluate(): verifying answer with OpenAI…")
            evaluation = self._agent.evaluate(problem)
            if evaluation.is_valid:
                log.info("  evaluate(): answer verified OK.")
                return problem

            last_error = evaluation.feedback
            log.warning("  evaluate(): attempt %d failed verification: %s", attempt + 1, last_error)
            if attempt == 0:
                continue

            raise ValueError(
                f"Answer verification failed after 2 attempts. "
                f"Last evaluator feedback: {last_error}"
            )

        # Unreachable, but satisfies type checkers
        raise RuntimeError("Unexpected state in _generate_and_verify")
