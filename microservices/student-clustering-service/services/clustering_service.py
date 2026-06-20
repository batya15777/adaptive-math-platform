from __future__ import annotations

import logging
import warnings
from collections import Counter

import numpy as np
from sklearn.cluster import KMeans
from sklearn.exceptions import ConvergenceWarning
from sklearn.metrics import silhouette_score

from app.config import get_settings
from models.schemas import (
    ClusterAssignment,
    ClusterSummary,
    ClusteringRequest,
    ClusteringResponse,
)
from services.feature_engineering import FeatureEngineer, StudentProfile

logger = logging.getLogger(__name__)

#המחלקה מקבלת את כל המספרים שהוכנו במחלקה feature engineering ומחלקת את התלמידים לאשכולות (Clusters).

class ClusteringService:
    """
    Groups students with K-Means based on their error/behaviour fingerprint.

    This class owns ONLY the ML logic (feature engineering delegation, choosing k,
    fitting, summarising). It knows nothing about HTTP — the router calls into it.
    """

    def __init__(self, feature_engineer: FeatureEngineer | None = None) -> None:
        self._settings = get_settings()
        self._fe = feature_engineer or FeatureEngineer()

    # -- public API ---------------------------------------------------------

    def cluster(self, request: ClusteringRequest) -> ClusteringResponse:
        if not request.students:
            raise ValueError("Request contains no students to cluster.")

        matrix = self._fe.build(request.students)
        logger.info(
            "Clustering %d students (requested k=%s)",
            matrix.X.shape[0],
            request.n_clusters,
        )

        k, labels, score = self._fit(matrix.X, request.n_clusters)

        assignments = [
            ClusterAssignment(user_id=uid, cluster_id=int(label))
            for uid, label in zip(matrix.user_ids, labels)
        ]
        clusters = self._summarise(labels, matrix.profiles, k)

        return ClusteringResponse(
            k=k,
            silhouette_score=score,
            assignments=assignments,
            clusters=clusters,
        )

    # -- choosing k + fitting ----------------------------------------------

    def _fit(self, X: np.ndarray, requested_k: int | None) -> tuple[int, np.ndarray, float | None]:
        n_samples = X.shape[0]

        # With 1-2 students there is nothing meaningful to separate.
        if n_samples <= 2:
            return 1, np.zeros(n_samples, dtype=int), None

        if requested_k is not None:
            k = max(1, min(requested_k, n_samples))
            if k == 1:
                return 1, np.zeros(n_samples, dtype=int), None
            labels = self._fit_labels(X, k)
            return k, labels, self._safe_silhouette(X, labels)

        return self._auto_select_k(X)

    def _auto_select_k(self, X: np.ndarray) -> tuple[int, np.ndarray, float | None]:
        """Pick k in [2, max_k] that maximises the silhouette score."""
        n_samples = X.shape[0]
        max_k = min(self._settings.max_k, n_samples - 1)

        best: tuple[float, int, np.ndarray] | None = None
        for k in range(2, max_k + 1):
            labels = self._fit_labels(X, k)
            if len(set(labels)) < 2:  # degenerate split — skip
                continue
            score = self._safe_silhouette(X, labels)
            if score is None:
                continue
            if best is None or score > best[0]:
                best = (score, k, labels)

        if best is None:
            # Nothing could be scored (e.g. all rows identical) — one group.
            return 1, np.zeros(n_samples, dtype=int), None

        logger.info("Auto-selected k=%d (silhouette=%.4f)", best[1], best[0])
        return best[1], best[2], best[0]

    def _fit_labels(self, X: np.ndarray, k: int) -> np.ndarray:
        # We deliberately probe several k values; asking for more clusters than there
        # are distinct points is expected and harmless, so silence that one warning.
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", category=ConvergenceWarning)
            return self._kmeans(k).fit_predict(X)

    def _kmeans(self, k: int) -> KMeans:
        return KMeans(n_clusters=k, random_state=self._settings.random_state, n_init=10)

    @staticmethod
    def _safe_silhouette(X: np.ndarray, labels: np.ndarray) -> float | None:
        try:
            return float(silhouette_score(X, labels))
        except ValueError:
            # Raised when there is < 2 valid clusters; treat as "not computable".
            return None

    # -- explainable summaries ---------------------------------------------

    def _summarise(
        self, labels: np.ndarray, profiles: list[StudentProfile], k: int
    ) -> list[ClusterSummary]:
        summaries: list[ClusterSummary] = []
        for cluster_id in range(k):
            members = [p for p, label in zip(profiles, labels) if label == cluster_id]
            if not members:
                continue
            avg_accuracy = float(np.mean([m.accuracy for m in members]))
            avg_difficulty = float(np.mean([m.avg_difficulty for m in members]))
            error_counter = Counter(err for m in members for err in m.error_patterns)
            top_errors = [err for err, _ in error_counter.most_common(3)]

            summaries.append(
                ClusterSummary(
                    cluster_id=cluster_id,
                    size=len(members),
                    label=self._label(avg_accuracy, top_errors),
                    avg_accuracy=round(avg_accuracy, 3),
                    avg_difficulty=round(avg_difficulty, 3),
                    top_error_patterns=top_errors,
                )
            )
        return summaries

    @staticmethod
    def _label(avg_accuracy: float, top_errors: list[str]) -> str:
        """A short, data-driven label so the cluster is meaningful at a glance."""
        if avg_accuracy >= 0.8:
            return "High performers"
        base = "Needs strong support" if avg_accuracy < 0.4 else "Developing"
        if top_errors:
            return f"{base} - mainly {top_errors[0]}"
        return base
