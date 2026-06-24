"""Tests for the request/response contract — the deterministic Pydantic validation
that protects the generator from malformed input: difficulty resolution, the
require-difficulty-or-band rule, range bounds and the strict 'forbid extra' policy.
"""
from __future__ import annotations

import pytest
from pydantic import ValidationError

from src.models import BAND_MIDPOINTS, GenerationRequest, LanguageCode, MathProblem


class TestGenerationRequest:
    def test_explicit_difficulty_is_kept_with_sensible_defaults(self):
        req = GenerationRequest(topic="fractions", theme="space", difficulty=7)
        assert req.difficulty == 7
        assert req.language == LanguageCode.en
        assert req.multiple_choice is False

    @pytest.mark.parametrize("band,expected", [("easy", 2), ("medium", 5), ("hard", 8)])
    def test_band_resolves_to_midpoint(self, band, expected):
        req = GenerationRequest(topic="t", theme="th", difficulty_band=band)
        assert req.difficulty == expected
        assert BAND_MIDPOINTS[band] == expected

    def test_explicit_difficulty_takes_precedence_over_band(self):
        req = GenerationRequest(topic="t", theme="th", difficulty=9, difficulty_band="easy")
        assert req.difficulty == 9

    def test_missing_both_difficulty_and_band_is_rejected(self):
        with pytest.raises(ValidationError):
            GenerationRequest(topic="t", theme="th")

    def test_empty_topic_is_rejected(self):
        with pytest.raises(ValidationError):
            GenerationRequest(topic="", theme="th", difficulty=5)

    def test_out_of_range_difficulty_is_rejected(self):
        with pytest.raises(ValidationError):
            GenerationRequest(topic="t", theme="th", difficulty=99)

    def test_unknown_field_is_rejected(self):
        with pytest.raises(ValidationError):
            GenerationRequest(topic="t", theme="th", difficulty=5, surprise="boom")


class TestMathProblem:
    def test_minimal_valid_problem(self):
        problem = MathProblem(
            question_text="How many planets?",
            correct_answer="26",
            step_by_step_solution=["Add the groups together."],
            difficulty_level=5,
        )
        assert problem.options is None
        assert problem.language == LanguageCode.en

    def test_difficulty_level_bounds_are_enforced(self):
        with pytest.raises(ValidationError):
            MathProblem(
                question_text="q",
                correct_answer="1",
                step_by_step_solution=["s"],
                difficulty_level=11,
            )
