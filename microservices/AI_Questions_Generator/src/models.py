from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class LanguageCode(str, Enum):
    en = "en"
    he = "he"
    ru = "ru"


class UserInfo(BaseModel):
    """
    Student information.
    ``extra = "allow"`` means callers can pass any additional fields (e.g. grade,
    learning_style, preferred_examples) and they will be forwarded to the LLM
    as extra personalisation context.
    """

    model_config = ConfigDict(extra="allow")

    name: str = Field(min_length=1, description="Student's first name")
    age: int = Field(ge=4, le=18, description="Student's age in years")


class LearningProfile(BaseModel):
    """
    Cluster-derived adaptation hints sent by the Java backend. Lets the generator
    gently target the student's cohort weakness and calibrate to their mastery.
    """

    model_config = ConfigDict(extra="ignore")

    cluster_label: str | None = Field(default=None, description="Human label of the student's ML cohort")
    focus_skill: str | None = Field(default=None, description="Skill/weakness to gently practise")
    mastery: str | None = Field(default=None, description="struggling | developing | strong")


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
    difficulty: int = Field(
        ge=1,
        le=10,
        description="Difficulty level from 1 (kindergarten-easy) to 10 (advanced/high-school)",
    )
    user_info: UserInfo = Field(description="Information about the student")
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
