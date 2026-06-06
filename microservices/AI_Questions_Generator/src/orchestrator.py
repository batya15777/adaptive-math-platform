from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from openai import OpenAI

from .agents import MathAgents
from .guardrails import ModerationGuardrail, check_output_guardrails
from .i18n import EMPTY_RESPONSE_MESSAGES
from .models import GenerationRequest, MathProblem, RouterDecision
from .settings import Settings


@dataclass(slots=True)
class QuestionGenerationOrchestrator:
    settings: Settings
    client: OpenAI | None
    _guardrail: Any = None
    _agents: Any = None

    def __post_init__(self) -> None:
        if self.client is None:
            return
        self._guardrail = ModerationGuardrail(self.client)
        self._agents = MathAgents(client=self.client, model=self.settings.openai_model)

    def generate(self, req: GenerationRequest) -> MathProblem:
        if self.client is None or self._guardrail is None or self._agents is None:
            raise RuntimeError("OPENAI_API_KEY not found. Set it in the container environment or pass --env-file .env when running Docker.")

        blocked = self._guardrail.check(req.context or req.topic, req.language)
        if blocked:
            raise ValueError(blocked)

        router_decision: RouterDecision = self._agents.route(req)
        problem = self._agents.generate(req, router_decision.route)
        evaluation = self._agents.evaluate(problem)

        if not evaluation.is_valid:
            raise ValueError(evaluation.feedback)

        final_question = MathProblem.model_validate(problem.model_dump())
        final_question.language = req.language
        final_question.question_text = check_output_guardrails(final_question.question_text)
        if final_question.question_text.startswith("System Error") or final_question.question_text == "I cannot process this request due to safety protocols":
            raise ValueError(EMPTY_RESPONSE_MESSAGES[req.language])

        return final_question


