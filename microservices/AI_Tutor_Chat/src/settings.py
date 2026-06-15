from __future__ import annotations

import os
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


class Settings:
    app_name: str = "AI Tutor Chat"
    openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    openai_enabled: bool = os.getenv("AI_TUTOR_ENABLE_OPENAI", "true").lower() == "true"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()

