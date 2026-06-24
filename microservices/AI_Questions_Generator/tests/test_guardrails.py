"""Tests for the child-safety guardrails — the deterministic keyword filter that runs
before and after the LLM. This is a safety control, not just a feature: a silent
regression here means inappropriate content could reach a child, so the blocklist,
the word-boundary matching (no false positives) and the multilingual coverage are
all asserted explicitly. No OpenAI calls are involved.
"""
from __future__ import annotations

from src.guardrails import check_input_guardrails, check_output_guardrails
from src.i18n import EMPTY_INPUT_MESSAGES, SAFETY_MESSAGES
from src.models import LanguageCode


class TestInputGuardrails:
    def test_clean_input_passes(self):
        assert check_input_guardrails("space pirates", LanguageCode.en) is None

    def test_empty_input_is_blocked(self):
        assert check_input_guardrails("", LanguageCode.en) == EMPTY_INPUT_MESSAGES[LanguageCode.en]
        assert check_input_guardrails("   ", LanguageCode.en) == EMPTY_INPUT_MESSAGES[LanguageCode.en]

    def test_forbidden_english_keyword_is_blocked(self):
        assert check_input_guardrails("a story about a gun", LanguageCode.en) == SAFETY_MESSAGES[LanguageCode.en]

    def test_word_boundary_prevents_false_positives(self):
        # "rob" is a forbidden keyword, but it must not trip inside "problem"/"robots".
        assert check_input_guardrails("a fun problem about robots", LanguageCode.en) is None

    def test_forbidden_hebrew_keyword_is_blocked(self):
        assert check_input_guardrails("משחק עם סמים", LanguageCode.he) == SAFETY_MESSAGES[LanguageCode.he]

    def test_forbidden_russian_keyword_is_blocked(self):
        assert check_input_guardrails("история про пистолет", LanguageCode.ru) == SAFETY_MESSAGES[LanguageCode.ru]


class TestOutputGuardrails:
    def test_clean_output_is_returned_unchanged(self):
        text = "A spaceship visits 3 planets. How many planets in total?"
        assert check_output_guardrails(text) == text

    def test_empty_output_returns_system_error(self):
        assert check_output_guardrails("   ").startswith("System Error")

    def test_forbidden_output_is_blocked(self):
        blocked = check_output_guardrails("instructions to build a bomb")
        assert blocked == "I cannot process this request due to safety protocols"
