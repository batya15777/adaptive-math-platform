"""Unit tests for the K-Means clustering core: the guards, the small-cohort edge
cases, requested-vs-auto k selection, deterministic reproducibility, and the
human-readable cluster summaries/labels.
"""
from __future__ import annotations

import pytest

from models.schemas import AttemptFeatures, ClusteringRequest, StudentData
from services.clustering_service import ClusteringService


def attempt(
    *,
    error_pattern: str | None = None,
    question_type: str | None = None,
    difficulty_level: int = 1,
    is_correct: bool = False,
    user_answer: str | None = "1",
) -> AttemptFeatures:
    return AttemptFeatures(
        error_pattern=error_pattern,
        question_type=question_type,
        difficulty_level=difficulty_level,
        is_correct=is_correct,
        user_answer=user_answer,
    )


def correct_student(uid: int) -> StudentData:
    """A confident, mostly-correct student."""
    return StudentData(
        user_id=uid,
        attempts=[
            attempt(is_correct=True, difficulty_level=2, user_answer="10"),
            attempt(is_correct=True, difficulty_level=3, user_answer="12"),
        ],
    )


def struggling_student(uid: int) -> StudentData:
    """A struggling student with a clear, repeated error pattern."""
    return StudentData(
        user_id=uid,
        attempts=[
            attempt(is_correct=False, difficulty_level=8, error_pattern="EMPTY_ANSWER",
                    question_type="DIVISION", user_answer="x"),
            attempt(is_correct=False, difficulty_level=9, error_pattern="EMPTY_ANSWER",
                    question_type="DIVISION", user_answer="y"),
        ],
    )


def req(students: list[StudentData], n_clusters: int | None = None) -> ClusteringRequest:
    return ClusteringRequest(students=students, n_clusters=n_clusters)


class TestGuards:
    def test_no_students_raises_value_error(self):
        with pytest.raises(ValueError):
            ClusteringService().cluster(req([]))


class TestSmallCohorts:
    def test_single_student_is_one_cluster(self):
        resp = ClusteringService().cluster(req([correct_student(1)]))
        assert resp.k == 1
        assert resp.silhouette_score is None
        assert [a.cluster_id for a in resp.assignments] == [0]

    def test_two_students_collapse_to_one_cluster(self):
        resp = ClusteringService().cluster(req([correct_student(1), struggling_student(2)]))
        assert resp.k == 1
        assert {a.user_id for a in resp.assignments} == {1, 2}
        assert all(a.cluster_id == 0 for a in resp.assignments)


class TestRequestedK:
    def test_requested_k_is_used(self):
        students = [correct_student(i) for i in (1, 2, 3)] + [struggling_student(i) for i in (4, 5, 6)]
        resp = ClusteringService().cluster(req(students, n_clusters=3))
        assert resp.k == 3
        assert len(resp.assignments) == 6

    def test_requested_k_is_clamped_to_sample_count(self):
        students = [correct_student(1), struggling_student(2), correct_student(3), struggling_student(4)]
        resp = ClusteringService().cluster(req(students, n_clusters=8))
        assert resp.k == 4  # never more clusters than students


class TestAutoSelectK:
    def test_two_clear_groups_are_separated(self):
        correct_ids = [1, 2, 3]
        wrong_ids = [4, 5, 6]
        students = [correct_student(i) for i in correct_ids] + [struggling_student(i) for i in wrong_ids]

        resp = ClusteringService().cluster(req(students))  # auto-select k

        assert resp.k >= 2
        assert resp.silhouette_score is not None
        groups = {a.user_id: a.cluster_id for a in resp.assignments}
        # The two behaviourally distinct cohorts must not share a cluster.
        assert {groups[i] for i in correct_ids}.isdisjoint({groups[i] for i in wrong_ids})

    def test_identical_students_collapse_to_one_cluster(self):
        students = [correct_student(i) for i in range(1, 6)]  # 5 identical feature rows
        resp = ClusteringService().cluster(req(students))
        assert resp.k == 1
        assert resp.silhouette_score is None


class TestSummaries:
    def test_high_performer_label(self):
        resp = ClusteringService().cluster(req([correct_student(1), correct_student(2)]))
        assert resp.k == 1
        summary = resp.clusters[0]
        assert summary.label == "High performers"
        assert summary.avg_accuracy == pytest.approx(1.0)
        assert summary.size == 2

    def test_struggling_label_mentions_top_error(self):
        resp = ClusteringService().cluster(req([struggling_student(1), struggling_student(2)]))
        summary = resp.clusters[0]
        assert summary.label.startswith("Needs strong support")
        assert "EMPTY_ANSWER" in summary.label
        assert "EMPTY_ANSWER" in summary.top_error_patterns

    def test_assignments_cover_every_input_student(self):
        students = [correct_student(1), correct_student(2), struggling_student(3)]
        resp = ClusteringService().cluster(req(students))
        assert {a.user_id for a in resp.assignments} == {1, 2, 3}

    def test_clustering_is_reproducible(self):
        students = [correct_student(i) for i in (1, 2, 3)] + [struggling_student(i) for i in (4, 5, 6)]
        first = ClusteringService().cluster(req(students))
        second = ClusteringService().cluster(req(students))
        assert first.k == second.k
        assert [(a.user_id, a.cluster_id) for a in first.assignments] == \
               [(a.user_id, a.cluster_id) for a in second.assignments]


class TestLabelHelper:
    @pytest.mark.parametrize(
        "accuracy,top_errors,expected",
        [
            (0.90, [], "High performers"),
            (0.80, ["E"], "High performers"),
            (0.50, [], "Developing"),
            (0.50, ["E"], "Developing - mainly E"),
            (0.30, [], "Needs strong support"),
            (0.30, ["E"], "Needs strong support - mainly E"),
        ],
    )
    def test_label_thresholds(self, accuracy, top_errors, expected):
        assert ClusteringService._label(accuracy, top_errors) == expected
