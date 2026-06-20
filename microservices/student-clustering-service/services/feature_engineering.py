from __future__ import annotations

import logging
from dataclasses import dataclass

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler

from models.schemas import AttemptFeatures, StudentData

logger = logging.getLogger(__name__)

#אלגוריתם K-Means עובד רק עם מספרים.
# הוא צריך מיקום מדויק (קואורדינטות) לכל תלמיד כדי למדוד את המרחק ביניהם.
# המחלקה הזו מקבלת את כל היסטוריית התרגולים של כל תלמיד (כולל טקסטים),
# והופכת כל תלמיד לשורה אחת ארוכה של מספרים (וקטור).#

@dataclass
class StudentProfile:
    """Readable aggregate stats for one student — used to label clusters afterwards."""

    user_id: int
    total_attempts: int
    accuracy: float
    avg_difficulty: float
    error_patterns: list[str]  # error labels from the student's incorrect attempts


@dataclass
class FeatureMatrix:
    """Output of feature engineering: aligned ids, matrix, names and readable profiles."""

    user_ids: list[int]
    X: np.ndarray
    feature_names: list[str]
    profiles: list[StudentProfile]


class FeatureEngineer:
    """
    Turns raw per-student attempt history into a numeric matrix that K-Means can use.

    Each student becomes ONE row, built from three concatenated blocks:

      A. Numeric behaviour  — accuracy, difficulty, timing, answer-quality ratios.
                              Standardised with ``StandardScaler`` so no single
                              large-scale feature (e.g. total_attempts) dominates.
      B. Error fingerprint  — TF-IDF over each student's "error document" (the
                              ``error_pattern`` strings). TF-IDF naturally
                              down-weights error labels everyone makes and
                              highlights distinctive weaknesses.
      C. Failed-topic finger— TF-IDF over the ``question_type`` of the attempts the
         print                student got *wrong* (where they actually struggle).

    Blocks B and C are how the categorical/textual fields (``error_pattern`` and
    ``question_type``) get vectorised into numbers — the requirement for feeding
    string data to a distance-based model.
    """

    NUMERIC_FEATURES = [
        "accuracy",
        "error_rate",
        "avg_difficulty",
        "max_difficulty",
        "total_attempts",
        "distinct_error_patterns",
        "avg_seconds_between_attempts",
        "empty_answer_rate",
        "non_numeric_answer_rate",
    ]

    # Cap on the gap between two attempts (seconds). Stops a student who came back
    # the next day from producing a meaningless multi-hour "thinking time".
    _MAX_GAP_SECONDS = 3600.0

    def __init__(self, error_block_weight: float = 1.0, qtype_block_weight: float = 0.7) -> None:
        self._error_weight = error_block_weight
        self._qtype_weight = qtype_block_weight

    # -- public API ---------------------------------------------------------

    def build(self, students: list[StudentData]) -> FeatureMatrix:
        if not students:
            raise ValueError("No students supplied for feature engineering.")

        user_ids: list[int] = []
        numeric_rows: list[list[float]] = []
        error_docs: list[str] = []
        qtype_docs: list[str] = []
        profiles: list[StudentProfile] = []

        for student in students:
            agg = self._aggregate_student(student)
            user_ids.append(student.user_id)
            numeric_rows.append(agg["numeric"])
            error_docs.append(agg["error_doc"])
            qtype_docs.append(agg["qtype_doc"])
            profiles.append(agg["profile"])

        numeric = np.asarray(numeric_rows, dtype=float)
        # StandardScaler needs >1 sample to estimate variance; skip it for a single row.
        numeric_scaled = StandardScaler().fit_transform(numeric) if len(numeric) > 1 else numeric

        error_block, error_names = self._tfidf_block(error_docs, prefix="err", weight=self._error_weight)
        qtype_block, qtype_names = self._tfidf_block(qtype_docs, prefix="wrongqt", weight=self._qtype_weight)

        blocks: list[np.ndarray] = [numeric_scaled]
        names: list[str] = list(self.NUMERIC_FEATURES)
        if error_block.size:
            blocks.append(error_block)
            names.extend(error_names)
        if qtype_block.size:
            blocks.append(qtype_block)
            names.extend(qtype_names)

        X = np.hstack(blocks)
        logger.info("Built feature matrix: %d students x %d features", X.shape[0], X.shape[1])
        return FeatureMatrix(user_ids=user_ids, X=X, feature_names=names, profiles=profiles)

    # -- per-student aggregation -------------------------------------------

    def _aggregate_student(self, student: StudentData) -> dict:
        attempts = student.attempts
        total = len(attempts)

        if total == 0:
            # Defensive: a student with no attempts becomes an all-zero row.
            return {
                "numeric": [0.0] * len(self.NUMERIC_FEATURES),
                "error_doc": "",
                "qtype_doc": "",
                "profile": StudentProfile(student.user_id, 0, 0.0, 0.0, []),
            }

        correct = sum(1 for a in attempts if a.is_correct)
        accuracy = correct / total

        difficulties = [a.difficulty_level for a in attempts]
        avg_difficulty = float(np.mean(difficulties))
        max_difficulty = float(np.max(difficulties))

        error_tokens = [self._norm(a.error_pattern) for a in attempts if a.error_pattern]
        # Question types of WRONG answers only — that is where the student struggles.
        wrong_qtypes = [
            self._norm(a.question_type)
            for a in attempts
            if (not a.is_correct) and a.question_type
        ]

        empty_answers = sum(1 for a in attempts if not (a.user_answer and a.user_answer.strip()))
        non_numeric = sum(
            1 for a in attempts if a.user_answer and not self._is_numeric(a.user_answer)
        )

        numeric = [
            accuracy,
            1.0 - accuracy,
            avg_difficulty,
            max_difficulty,
            float(total),
            float(len(set(error_tokens))),
            self._avg_seconds_between(attempts),
            empty_answers / total,
            non_numeric / total,
        ]

        profile = StudentProfile(
            user_id=student.user_id,
            total_attempts=total,
            accuracy=accuracy,
            avg_difficulty=avg_difficulty,
            error_patterns=error_tokens,
        )
        return {
            "numeric": numeric,
            "error_doc": " ".join(error_tokens),
            "qtype_doc": " ".join(wrong_qtypes),
            "profile": profile,
        }

    # -- helpers ------------------------------------------------------------

    @classmethod
    def _avg_seconds_between(cls, attempts: list[AttemptFeatures]) -> float:
        times = sorted(a.answered_at for a in attempts if a.answered_at is not None)
        if len(times) < 2:
            return 0.0
        gaps = [
            min((times[i] - times[i - 1]).total_seconds(), cls._MAX_GAP_SECONDS)
            for i in range(1, len(times))
        ]
        return float(np.mean(gaps))

    @staticmethod
    def _tfidf_block(docs: list[str], prefix: str, weight: float) -> tuple[np.ndarray, list[str]]:
        """TF-IDF a list of token documents; returns an empty block if the vocab is empty."""
        if not any(doc.strip() for doc in docs):
            return np.empty((len(docs), 0)), []
        # token_pattern keeps whole labels intact (e.g. CONFUSED_SUB_WITH_ADD).
        vectorizer = TfidfVectorizer(token_pattern=r"[^\s]+", lowercase=False)
        matrix = vectorizer.fit_transform(docs).toarray() * weight
        names = [f"{prefix}:{token}" for token in vectorizer.get_feature_names_out()]
        return matrix, names

    @staticmethod
    def _norm(value: str) -> str:
        return value.strip().upper().replace(" ", "_")

    @staticmethod
    def _is_numeric(value: str) -> bool:
        try:
            float(value.strip())
            return True
        except (ValueError, AttributeError):
            return False
