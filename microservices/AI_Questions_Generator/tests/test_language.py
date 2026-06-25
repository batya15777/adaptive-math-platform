"""Language-correctness tests.

Two layers:
1. The pure `language_mismatch` heuristic (deterministic, no LLM).
2. The orchestrator's retry behaviour when the generator returns the wrong language —
   it must reject the first (English) attempt for a Hebrew request and retry, exactly
   like a failed answer-verification. We replace the agent (the OpenAI boundary) with a
   scripted fake so the control flow is deterministic.
"""
from __future__ import annotations

import pytest

from src.language_check import language_mismatch
from src.models import EvaluationResult, GenerationRequest, LanguageCode, MathProblem
from src.orchestrator import QuestionGenerationOrchestrator


# --- 1. unit tests for the heuristic --------------------------------------

class TestLanguageMismatch:
    def test_hebrew_text_passes_hebrew_request(self):
        assert language_mismatch("כמה כוכבים יש לNAME בסך הכול?", LanguageCode.he) is None

    def test_english_text_fails_hebrew_request(self):
        assert language_mismatch("How many stars does NAME have?", LanguageCode.he) is not None

    def test_russian_text_passes_russian_request(self):
        assert language_mismatch("Сколько звёзд у NAME всего?", LanguageCode.ru) is None

    def test_english_text_fails_russian_request(self):
        assert language_mismatch("How many stars does NAME have?", LanguageCode.ru) is not None

    def test_english_with_name_and_digits_passes_english_request(self):
        # NAME placeholder + digits are script-neutral and must not trip the English check.
        assert language_mismatch("NAME counted 26 stars. How many are left?", LanguageCode.en) is None

    def test_hebrew_text_fails_english_request(self):
        assert language_mismatch("כמה כוכבים?", LanguageCode.en) is not None


# --- test doubles (mirrors tests/test_orchestrator.py) --------------------

class _Settings:
    openai_model = "test-model"


class FakeGuardrail:
    def check(self, text: str, language) -> str | None:
        return None


class FakeAgent:
    """Consumes `generate_results`/`evaluate_results` one item per call; an Exception
    item is raised, anything else returned."""

    def __init__(self, generate_results=None, evaluate_results=None) -> None:
        self._generate = list(generate_results or [])
        self._evaluate = list(evaluate_results or [])
        self.generate_calls = 0
        self.evaluate_calls = 0

    def generate(self, req):
        self.generate_calls += 1
        item = self._generate.pop(0)
        if isinstance(item, Exception):
            raise item
        return item

    def evaluate(self, problem):
        self.evaluate_calls += 1
        item = self._evaluate.pop(0)
        if isinstance(item, Exception):
            raise item
        return item


def _problem(question_text: str) -> MathProblem:
    return MathProblem(
        question_text=question_text,
        correct_answer="3",
        step_by_step_solution=["Count them."],
        difficulty_level=5,
    )


def _ok_eval() -> EvaluationResult:
    return EvaluationResult(is_valid=True, calculated_answer="3", generated_answer="3", feedback="ok")


def _make_orchestrator(agent: FakeAgent) -> QuestionGenerationOrchestrator:
    orch = QuestionGenerationOrchestrator(settings=_Settings(), client=None)
    orch.client = object()
    orch._guardrail = FakeGuardrail()
    orch._agent = agent
    return orch


# --- 2. orchestrator retry on wrong language ------------------------------

class TestOrchestratorLanguageRetry:
    def test_english_first_attempt_is_rejected_and_retried_in_hebrew(self):
        agent = FakeAgent(
            generate_results=[
                _problem("How many stars does NAME have?"),   # wrong language — rejected
                _problem("כמה כוכבים יש לNAME בסך הכול?"),      # correct — accepted
            ],
            evaluate_results=[_ok_eval()],
        )
        orch = _make_orchestrator(agent)
        req = GenerationRequest(topic="addition", theme="space", difficulty=5, language=LanguageCode.he)

        result = orch.generate(req)

        assert "כוכבים" in result.question_text
        assert agent.generate_calls == 2          # first attempt rejected, regenerated
        assert agent.evaluate_calls == 1          # only the valid-language problem is verified

    def test_wrong_language_twice_raises(self):
        agent = FakeAgent(
            generate_results=[
                _problem("English one NAME"),
                _problem("English two NAME"),
            ],
        )
        orch = _make_orchestrator(agent)
        req = GenerationRequest(topic="addition", theme="space", difficulty=5, language=LanguageCode.he)

        with pytest.raises(ValueError):
            orch.generate(req)
        assert agent.generate_calls == 2
        assert agent.evaluate_calls == 0          # never reaches verification
