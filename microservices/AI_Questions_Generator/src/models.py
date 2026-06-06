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


class MathProblem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    question_text: str
    correct_answer: str
    step_by_step_solution: list[str]
    difficulty_level: int = Field(ge=1, le=10)
    language: LanguageCode = Field(default=LanguageCode.en, description="Output language")


class EvaluationResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    is_valid: bool
    calculated_answer: str
    generated_answer: str
    feedback: str
