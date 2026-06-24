"""Unit tests for the feature engineering stage — the deterministic core that turns
raw attempt history into the numeric matrix K-Means consumes. Most assertions go
through ``_aggregate_student`` so they check the RAW per-student numbers before
``StandardScaler`` rescales them in ``build``.
"""
from __future__ import annotations

from datetime import datetime, timedelta

import pytest

from models.schemas import AttemptFeatures, StudentData
from services.feature_engineering import FeatureEngineer


def attempt(
    *,
    error_pattern: str | None = None,
    question_type: str | None = None,
    difficulty_level: int = 1,
    is_correct: bool = False,
    user_answer: str | None = "1",
    answered_at: datetime | None = None,
) -> AttemptFeatures:
    return AttemptFeatures(
        error_pattern=error_pattern,
        question_type=question_type,
        difficulty_level=difficulty_level,
        is_correct=is_correct,
        user_answer=user_answer,
        answered_at=answered_at,
    )


def student(user_id: int, attempts: list[AttemptFeatures]) -> StudentData:
    return StudentData(user_id=user_id, attempts=attempts)


# Index of each engineered feature inside the numeric vector, for readable assertions.
F = {name: i for i, name in enumerate(FeatureEngineer.NUMERIC_FEATURES)}


class TestAggregateStudent:
    def test_zero_attempts_is_all_zero_row(self):
        agg = FeatureEngineer()._aggregate_student(student(1, []))
        assert agg["numeric"] == [0.0] * len(FeatureEngineer.NUMERIC_FEATURES)
        assert agg["error_doc"] == ""
        assert agg["qtype_doc"] == ""
        assert agg["profile"].total_attempts == 0
        assert agg["profile"].accuracy == 0.0

    def test_accuracy_and_error_rate(self):
        attempts = [
            attempt(is_correct=True),
            attempt(is_correct=True),
            attempt(is_correct=True),
            attempt(is_correct=False, error_pattern="E"),
        ]
        agg = FeatureEngineer()._aggregate_student(student(1, attempts))
        assert agg["numeric"][F["accuracy"]] == pytest.approx(0.75)
        assert agg["numeric"][F["error_rate"]] == pytest.approx(0.25)
        assert agg["profile"].accuracy == pytest.approx(0.75)

    def test_difficulty_aggregates(self):
        attempts = [attempt(difficulty_level=2), attempt(difficulty_level=6)]
        numeric = FeatureEngineer()._aggregate_student(student(1, attempts))["numeric"]
        assert numeric[F["avg_difficulty"]] == pytest.approx(4.0)
        assert numeric[F["max_difficulty"]] == pytest.approx(6.0)

    def test_error_tokens_are_normalised(self):
        attempts = [attempt(is_correct=False, error_pattern="confused sub")]
        agg = FeatureEngineer()._aggregate_student(student(1, attempts))
        assert agg["error_doc"] == "CONFUSED_SUB"
        assert agg["profile"].error_patterns == ["CONFUSED_SUB"]

    def test_distinct_error_pattern_count(self):
        attempts = [
            attempt(is_correct=False, error_pattern="E1"),
            attempt(is_correct=False, error_pattern="E1"),
            attempt(is_correct=False, error_pattern="E2"),
        ]
        numeric = FeatureEngineer()._aggregate_student(student(1, attempts))["numeric"]
        assert numeric[F["distinct_error_patterns"]] == pytest.approx(2.0)

    def test_only_wrong_answer_question_types_are_kept(self):
        attempts = [
            attempt(is_correct=True, question_type="ADDITION"),      # correct -> excluded
            attempt(is_correct=False, question_type="SUBTRACTION"),  # wrong -> kept
        ]
        qtype_doc = FeatureEngineer()._aggregate_student(student(1, attempts))["qtype_doc"]
        assert qtype_doc == "SUBTRACTION"

    def test_empty_and_non_numeric_answer_rates(self):
        attempts = [
            attempt(user_answer="42"),    # numeric, non-empty
            attempt(user_answer="abc"),   # non-numeric, non-empty
            attempt(user_answer=None),    # empty
        ]
        numeric = FeatureEngineer()._aggregate_student(student(1, attempts))["numeric"]
        assert numeric[F["empty_answer_rate"]] == pytest.approx(1 / 3)        # the None answer
        assert numeric[F["non_numeric_answer_rate"]] == pytest.approx(1 / 3)  # the "abc" answer


class TestTiming:
    def test_single_timestamp_has_zero_gap(self):
        attempts = [attempt(answered_at=datetime(2026, 1, 1, 10, 0, 0))]
        numeric = FeatureEngineer()._aggregate_student(student(1, attempts))["numeric"]
        assert numeric[F["avg_seconds_between_attempts"]] == 0.0

    def test_average_gap_between_attempts(self):
        base = datetime(2026, 1, 1, 10, 0, 0)
        attempts = [
            attempt(answered_at=base),
            attempt(answered_at=base + timedelta(seconds=30)),
            attempt(answered_at=base + timedelta(seconds=90)),  # +60
        ]
        numeric = FeatureEngineer()._aggregate_student(student(1, attempts))["numeric"]
        assert numeric[F["avg_seconds_between_attempts"]] == pytest.approx(45.0)  # mean(30, 60)

    def test_huge_gap_is_capped_at_one_hour(self):
        base = datetime(2026, 1, 1, 10, 0, 0)
        attempts = [
            attempt(answered_at=base),
            attempt(answered_at=base + timedelta(hours=5)),
        ]
        numeric = FeatureEngineer()._aggregate_student(student(1, attempts))["numeric"]
        assert numeric[F["avg_seconds_between_attempts"]] == pytest.approx(3600.0)


class TestBuildMatrix:
    def test_empty_students_raises(self):
        with pytest.raises(ValueError):
            FeatureEngineer().build([])

    def test_matrix_shape_and_id_alignment(self):
        students = [
            student(10, [attempt(is_correct=False, error_pattern="E1", question_type="ADD")]),
            student(20, [attempt(is_correct=True)]),
        ]
        matrix = FeatureEngineer().build(students)
        assert matrix.user_ids == [10, 20]
        assert matrix.X.shape[0] == 2
        assert matrix.X.shape[1] >= len(FeatureEngineer.NUMERIC_FEATURES)
        assert len(matrix.profiles) == 2

    def test_all_correct_cohort_has_only_the_numeric_block(self):
        # No error_pattern and no wrong question_type anywhere -> both TF-IDF blocks empty.
        students = [
            student(1, [attempt(is_correct=True, user_answer="5")]),
            student(2, [attempt(is_correct=True, user_answer="6")]),
        ]
        matrix = FeatureEngineer().build(students)
        assert matrix.X.shape[1] == len(FeatureEngineer.NUMERIC_FEATURES)

    def test_error_and_qtype_text_add_tfidf_columns(self):
        students = [
            student(1, [attempt(is_correct=False, error_pattern="E1", question_type="ADD")]),
            student(2, [attempt(is_correct=False, error_pattern="E2", question_type="SUB")]),
        ]
        matrix = FeatureEngineer().build(students)
        assert matrix.X.shape[1] > len(FeatureEngineer.NUMERIC_FEATURES)
        assert any(name.startswith("err:") for name in matrix.feature_names)
        assert any(name.startswith("wrongqt:") for name in matrix.feature_names)


class TestStaticHelpers:
    def test_norm_uppercases_and_underscores(self):
        assert FeatureEngineer._norm("  confused sub  ") == "CONFUSED_SUB"

    @pytest.mark.parametrize(
        "value,expected",
        [("3.5", True), ("42", True), ("-7", True), ("abc", False), ("", False), ("3/4", False)],
    )
    def test_is_numeric(self, value, expected):
        assert FeatureEngineer._is_numeric(value) is expected
