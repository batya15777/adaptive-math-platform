from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

# ---------------------------------------------------------------------------
# Request models — what the Spring Boot backend POSTs to /clustering/run.
# `extra="ignore"` keeps the contract forgiving: the Java side may add fields
# (e.g. sub_subject_id) without breaking this service.
# ---------------------------------------------------------------------------


class AttemptFeatures(BaseModel):
    """A single exercise attempt, mirroring the Java ``ExerciseAttempt`` entity."""

    model_config = ConfigDict(extra="ignore")

    error_pattern: str | None = Field(
        default=None,
        description="Diagnosed error label, e.g. CONFUSED_SUB_WITH_ADD. Null when the answer was correct.",
    )
    question_type: str | None = Field(
        default=None, description="e.g. SIMPLE_ADDITION, CARRYING_SUBTRACTION."
    )
    difficulty_level: int = Field(default=1, ge=0, description="Difficulty band of the question.")
    is_correct: bool = Field(default=False, description="Whether the student answered correctly.")
    user_answer: str | None = Field(default=None, description="Raw answer the student typed.")
    answered_at: datetime | None = Field(default=None, description="When the attempt was submitted.")


class StudentData(BaseModel):
    """All attempts collected for one student."""

    model_config = ConfigDict(extra="ignore")

    user_id: int = Field(description="Primary key of the user in the Java DB.")
    attempts: list[AttemptFeatures] = Field(default_factory=list)


class ClusteringRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    students: list[StudentData] = Field(default_factory=list)
    n_clusters: int | None = Field(
        default=None,
        ge=2,
        description="Optional fixed k. When omitted, k is chosen automatically by silhouette score.",
    )


# ---------------------------------------------------------------------------
# Response models — what this service returns to Spring Boot.
# ---------------------------------------------------------------------------


class ClusterAssignment(BaseModel):
    """The core mapping the question generator needs: which group a student is in."""

    user_id: int
    cluster_id: int


class ClusterSummary(BaseModel):
    """Human-readable profile of a cluster — useful for the future admin dashboard."""

    cluster_id: int
    size: int
    label: str = Field(description="Auto-generated descriptive label for the group.")
    avg_accuracy: float
    avg_difficulty: float
    top_error_patterns: list[str] = Field(
        default_factory=list, description="Most common error labels inside this cluster."
    )


class ClusteringResponse(BaseModel):
    k: int = Field(description="Number of clusters actually used.")
    silhouette_score: float | None = Field(
        default=None, description="Quality of the clustering in [-1, 1]; null when not computable."
    )
    assignments: list[ClusterAssignment]
    clusters: list[ClusterSummary]
