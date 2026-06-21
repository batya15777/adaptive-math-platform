from __future__ import annotations

from enum import Enum
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class LanguageCode(str, Enum):
    en = "en"
    he = "he"
    ru = "ru"


class LearningProfile(BaseModel):
    """
    Cluster-derived adaptation hints sent by the Java backend. Lets the generator
    gently target the student's cohort weakness and calibrate to their mastery.
    """

    model_config = ConfigDict(extra="ignore")

    cluster_label: str | None = Field(default=None, description="Human label of the student's ML cohort")
    focus_skill: str | None = Field(default=None, description="Skill/weakness to gently practise")
    mastery: str | None = Field(default=None, description="struggling | developing | strong")


BAND_MIDPOINTS: dict[str, int] = {"easy": 2, "medium": 5, "hard": 8}


class GenerationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    topic: str = Field(
        min_length=1,
        description="Subject topic (e.g. fractions, history, geometry, algebra, biology)",
    )
    theme: str = Field(
        min_length=1,
        description="Creative narrative theme to wrap the question in (e.g. pirates, space, dinosaurs, wizards)",
    )
    difficulty: int | None = Field(
        default=None,
        ge=1,
        le=10,
        description="Difficulty level from 1 (kindergarten-easy) to 10 (advanced/high-school). "
                    "Takes precedence over difficulty_band when both are provided.",
    )
    difficulty_band: Literal["easy", "medium", "hard"] | None = Field(
        default=None,
        description=(
            "Coarse difficulty band. Maps to a midpoint when 'difficulty' is absent: "
            "easy→2, medium→5, hard→8. When both are provided, 'difficulty' takes precedence "
            "and the band is used as a supplementary prompt hint."
        ),
    )
    language: LanguageCode = Field(
        default=LanguageCode.en,
        description="Output language for the generated question",
    )
    multiple_choice: bool = Field(
        default=False,
        description="When true, the response includes 4 answer options; the student picks one.",
    )
    learning_profile: LearningProfile | None = Field(
        default=None,
        description="Optional cluster-derived adaptation hints for personalising the question.",
    )

    @model_validator(mode="after")
    def resolve_difficulty(self) -> "GenerationRequest":
        if self.difficulty is None and self.difficulty_band is None:
            raise ValueError("Provide either 'difficulty' (1–10) or 'difficulty_band' (easy/medium/hard).")
        if self.difficulty is None:
            self.difficulty = BAND_MIDPOINTS[self.difficulty_band]
        return self


class MathProblem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    question_text: str
    correct_answer: str = Field(
        description=(
            "Minimal, directly-comparable value — digits only for numbers (e.g. '26', '3.5', '3/4'), "
            "shortest definitive phrase for text (e.g. '1066', 'Julius Caesar'). "
            "No prose, no units, no explanation. "
            "For multiple-choice this value matches one of the options exactly."
        )
    )
    options: list[str] | None = Field(
        default=None,
        description=(
            "Present only when multiple_choice=true. "
            "Exactly 4 answer options; correct_answer matches one of them character-for-character."
        ),
    )
    step_by_step_solution: list[str]
    difficulty_level: int = Field(ge=1, le=10)
    language: LanguageCode = Field(default=LanguageCode.en, description="Output language")


class EvaluationResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    is_valid: bool
    calculated_answer: str
    generated_answer: str
    feedback: str
