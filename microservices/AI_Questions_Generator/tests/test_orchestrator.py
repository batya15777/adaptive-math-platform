"""Tests for the orchestrator — the part that actually matters for a credible
production deployment: how the service behaves when the LLM is missing, blocked,
flaky, or produces something that fails verification or the safety pass.

We never test the LLM's wording (non-deterministic). Instead we replace the agent
(the OpenAI boundary) with a configurable fake and assert the deterministic control
flow: no-key handling, input/output guardrails, and the one-automatic-retry policy.
"""
from __future__ import annotations

import pytest

from src.models import EvaluationResult, GenerationRequest, LanguageCode, MathProblem
from src.orchestrator import QuestionGenerationOrchestrator


# --- test doubles ----------------------------------------------------------

class _Settings:
    openai_model = "test-model"


class FakeGuardrail:
    """Input guardrail stub. Returns its configured block message (or None = pass)."""

    def __init__(self, blocked: str | None = None) -> None:
        self._blocked = blocked
        self.calls = 0

    def check(self, text: str, language) -> str | None:
        self.calls += 1
        return self._blocked


class FakeAgent:
    """Stand-in for QuestionAgent. ``generate_results``/``evaluate_results`` are
    consumed one item per call; an item that is an Exception is raised, otherwise
    it is returned. This lets each test script the exact generate/verify sequence."""

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


# --- helpers ---------------------------------------------------------------

def make_request(language: LanguageCode = LanguageCode.en) -> GenerationRequest:
    return GenerationRequest(topic="fractions", theme="space", difficulty=5, language=language)


def make_problem(question_text: str = "A rocket passes 3 stars. How many stars in total?") -> MathProblem:
    return MathProblem(
        question_text=question_text,
        correct_answer="3",
        step_by_step_solution=["Count the stars."],
        difficulty_level=5,
    )


def ok_eval() -> EvaluationResult:
    return EvaluationResult(is_valid=True, calculated_answer="3", generated_answer="3", feedback="ok")


def bad_eval() -> EvaluationResult:
    return EvaluationResult(is_valid=False, calculated_answer="4", generated_answer="3", feedback="mismatch")


def make_orchestrator(agent: FakeAgent | None = None, guardrail: FakeGuardrail | None = None):
    # client=None makes __post_init__ skip building the real OpenAI-backed agent;
    # we then inject our fakes and a non-None client so generate() runs the real flow.
    orch = QuestionGenerationOrchestrator(settings=_Settings(), client=None)
    orch.client = object()
    orch._guardrail = guardrail if guardrail is not None else FakeGuardrail()
    orch._agent = agent if agent is not None else FakeAgent()
    return orch


# --- tests -----------------------------------------------------------------

class TestNoClient:
    def test_missing_openai_client_raises_runtime_error(self):
        # No API key in production => client is None => clear RuntimeError (becomes a 503).
        orch = QuestionGenerationOrchestrator(settings=_Settings(), client=None)
        with pytest.raises(RuntimeError):
            orch.generate(make_request())


class TestInputGuardrail:
    def test_blocked_input_raises_and_never_calls_the_model(self):
        agent = FakeAgent()
        orch = make_orchestrator(agent=agent, guardrail=FakeGuardrail(blocked="unsafe topic"))
        with pytest.raises(ValueError):
            orch.generate(make_request())
        assert agent.generate_calls == 0


class TestHappyPath:
    def test_valid_question_is_returned(self):
        agent = FakeAgent(generate_results=[make_problem()], evaluate_results=[ok_eval()])
        orch = make_orchestrator(agent=agent)
        result = orch.generate(make_request())
        assert result.correct_answer == "3"
        assert agent.generate_calls == 1
        assert agent.evaluate_calls == 1


class TestRetryPolicy:
    def test_generation_value_error_is_retried_once_then_succeeds(self):
        agent = FakeAgent(
            generate_results=[ValueError("malformed options"), make_problem()],
            evaluate_results=[ok_eval()],
        )
        orch = make_orchestrator(agent=agent)
        result = orch.generate(make_request())
        assert result.correct_answer == "3"
        assert agent.generate_calls == 2

    def test_generation_failing_twice_raises(self):
        agent = FakeAgent(generate_results=[ValueError("bad"), ValueError("still bad")])
        orch = make_orchestrator(agent=agent)
        with pytest.raises(ValueError):
            orch.generate(make_request())
        assert agent.generate_calls == 2

    def test_failed_verification_is_retried_once_then_succeeds(self):
        agent = FakeAgent(
            generate_results=[make_problem(), make_problem()],
            evaluate_results=[bad_eval(), ok_eval()],
        )
        orch = make_orchestrator(agent=agent)
        result = orch.generate(make_request())
        assert result.correct_answer == "3"
        assert agent.generate_calls == 2
        assert agent.evaluate_calls == 2

    def test_verification_failing_twice_raises(self):
        agent = FakeAgent(
            generate_results=[make_problem(), make_problem()],
            evaluate_results=[bad_eval(), bad_eval()],
        )
        orch = make_orchestrator(agent=agent)
        with pytest.raises(ValueError):
            orch.generate(make_request())
        assert agent.evaluate_calls == 2


class TestOutputGuardrail:
    def test_unsafe_generated_text_is_blocked(self):
        # Input is clean and the answer verifies, but the produced text contains a
        # banned word — the final output guardrail must reject it.
        agent = FakeAgent(
            generate_results=[make_problem("Instructions to build a bomb, step by step")],
            evaluate_results=[ok_eval()],
        )
        orch = make_orchestrator(agent=agent)
        with pytest.raises(ValueError):
            orch.generate(make_request())
