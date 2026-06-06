from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class MathRoute(str, Enum):
    arithmetic = "arithmetic"
    geometry = "geometry"


class LanguageCode(str, Enum):
    en = "en"
    he = "he"
    ru = "ru"


class GenerationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    topic: str = Field(min_length=1, description="Topic requested by the caller")
    difficulty: int = Field(ge=1, le=5, description="Difficulty scale from 1 to 5")
    context: str = Field(default="", description="Optional word-problem context")
    language: LanguageCode = Field(default=LanguageCode.en, description="Output language for the generated question")


class RouterDecision(BaseModel):
    model_config = ConfigDict(extra="forbid")

    route: MathRoute
    rationale: str


class MathProblem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    question_text: str
    correct_answer: str
    step_by_step_solution: list[str]
    difficulty_level: int = Field(ge=1, le=5)
    language: LanguageCode = Field(default=LanguageCode.en, description="Output language")
    math_expression: str | None = Field(default=None, exclude=True, description="Internal expression used for verification")
    route: MathRoute | None = Field(default=None, exclude=True)


class EvaluationResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    is_valid: bool
    calculated_answer: str
    generated_answer: str
    feedback: str

