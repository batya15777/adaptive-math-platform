from __future__ import annotations

import logging

from fastapi import Depends, FastAPI, HTTPException
from openai import OpenAI

from src.models import GenerationRequest, MathProblem
from src.orchestrator import QuestionGenerationOrchestrator
from src.security import verify_api_key
from src.settings import get_settings

# Make sure our own log messages show up in the uvicorn terminal at INFO level.
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
)
log = logging.getLogger("ai_questions")

settings = get_settings()

# Log how the OpenAI key was resolved (only the last 4 chars — never the full secret).
if settings.openai_api_key:
    log.info(
        "OpenAI client configured. Model=%s | key ...%s",
        settings.openai_model,
        settings.openai_api_key[-4:],
    )
else:
    log.warning("OPENAI_API_KEY is NOT set — /generate-question will return 503.")

client = OpenAI(api_key=settings.openai_api_key) if settings.openai_api_key else None
orchestrator = QuestionGenerationOrchestrator(settings=settings, client=client)

app = FastAPI(title=settings.app_name)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/generate-question", response_model=MathProblem,
          dependencies=[Depends(verify_api_key)])
def generate_question(req: GenerationRequest) -> MathProblem:
    log.info(
        "→ /generate-question | topic=%r theme=%r difficulty=%s band=%s lang=%s mc=%s",
        req.topic, req.theme, req.difficulty, req.difficulty_band,
        req.language.value, req.multiple_choice,
    )
    try:
        problem = orchestrator.generate(req)
        log.info("✓ Question generated successfully (answer=%r)", problem.correct_answer)
        return problem
    except RuntimeError as exc:
        log.error("✗ 503 RuntimeError: %s", exc)
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except ValueError as exc:
        log.warning("✗ 400 ValueError (guardrail / validation): %s", exc)
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        # Full traceback to the terminal so the real failure point is visible.
        log.exception("✗ 500 Unhandled exception while generating question")
        raise HTTPException(status_code=500, detail=f"{type(exc).__name__}: {exc}") from exc

#     run with: uv run uvicorn main:app --reload
