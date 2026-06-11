from __future__ import annotations

from fastapi import FastAPI, HTTPException
from openai import OpenAI

from src.models import GenerationRequest, MathProblem
from src.orchestrator import QuestionGenerationOrchestrator
from src.settings import get_settings

settings = get_settings()
client = OpenAI(api_key=settings.openai_api_key) if settings.openai_api_key else None
orchestrator = QuestionGenerationOrchestrator(settings=settings, client=client)

app = FastAPI(title=settings.app_name)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/generate-question", response_model=MathProblem)
def generate_question(req: GenerationRequest) -> MathProblem:
    try:
        return orchestrator.generate(req)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc

#     run with: uv run uvicorn main:app --reload
