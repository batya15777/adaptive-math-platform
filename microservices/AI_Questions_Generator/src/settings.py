from __future__ import annotations

import os
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


class Settings:
    app_name: str = "AI Questions Generator"
    openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    moderation_model: str = os.getenv("OPENAI_MODERATION_MODEL", "omni-moderation-latest")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()

