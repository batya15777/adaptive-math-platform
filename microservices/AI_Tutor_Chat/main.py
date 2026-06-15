from __future__ import annotations

from fastapi import FastAPI
from openai import OpenAI

from src.models import TutorChatRequest, TutorChatResponse
from src.provider import OpenAITutorProvider
from src.service import TutorService
from src.settings import get_settings

settings = get_settings()
provider = None
if settings.openai_enabled and settings.openai_api_key:
    provider = OpenAITutorProvider(
        client=OpenAI(api_key=settings.openai_api_key),
        model=settings.openai_model,
    )

app = FastAPI(title=settings.app_name)
tutor_service = TutorService(provider=provider)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/chat", response_model=TutorChatResponse)
def chat(request: TutorChatRequest) -> TutorChatResponse:
    return tutor_service.reply(request)


# Run locally with: uv run uvicorn main:app --reload --port 8001
