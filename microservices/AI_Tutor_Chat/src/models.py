from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class LanguageCode(str, Enum):
    en = "en"
    he = "he"
    ru = "ru"


class QuestionSource(str, Enum):
    manual = "MANUAL"
    ai = "AI"


class TutorAction(str, Enum):
    hint = "HINT"
    redirect = "REDIRECT"
    safety = "SAFETY"


class QuestionContext(BaseModel):
    model_config = ConfigDict(extra="forbid")

    question_id: int | None = Field(default=None, ge=1)
    expression: str = Field(min_length=1)
    correct_answer: str = Field(min_length=1)
    solution_steps: list[str] = Field(min_length=1)
    options: list[str] | None = None
    subject: str = Field(min_length=1)
    sub_subject: str = Field(min_length=1)
    difficulty_level: int = Field(ge=1, le=10)
    language: LanguageCode = LanguageCode.en
    source: QuestionSource


class StudentContext(BaseModel):
    model_config = ConfigDict(extra="allow")

    student_id: int | None = Field(default=None, ge=1)
    age: int = Field(ge=4, le=18)
    current_level: int | None = Field(default=None, ge=1)
    weakness_type: str | None = None


class ConversationMessage(BaseModel):
    model_config = ConfigDict(extra="forbid")

    role: str = Field(pattern="^(student|tutor)$")
    content: str = Field(min_length=1, max_length=2000)


class TutorChatRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    question: QuestionContext
    student: StudentContext
    student_message: str = Field(min_length=1, max_length=2000)
    conversation_history: list[ConversationMessage] = Field(
        default_factory=list,
        max_length=20,
    )
    guidance_level: int = Field(default=0, ge=0, le=5)


class TutorChatResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    message: str
    guidance_level: int = Field(ge=0, le=5)
    action: TutorAction

