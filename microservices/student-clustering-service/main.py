from __future__ import annotations

import logging

import uvicorn
from fastapi import FastAPI

from app.config import get_settings
from app.logging_config import configure_logging
from routers.clustering import router as clustering_router

configure_logging()
logger = logging.getLogger(__name__)
settings = get_settings()

app = FastAPI(title=settings.app_name, version="1.0.0")
app.include_router(clustering_router)


@app.get("/health", tags=["meta"])
def health() -> dict[str, str]:
    """Liveness probe — used by the Java side to detect whether the service is up."""
    return {"status": "ok", "service": settings.app_name}


if __name__ == "__main__":
    # Run with:  uv run uvicorn main:app --reload --port 8001
    logger.info("Starting %s on %s:%d", settings.app_name, settings.host, settings.port)
    uvicorn.run(app, host=settings.host, port=settings.port)
