from __future__ import annotations

import os
from functools import lru_cache

from dotenv import load_dotenv

# override=True so the project's .env is the single source of truth, even when a
# stale OPENAI_API_KEY is already present in the OS/shell environment.
load_dotenv(override=True)


class Settings:
    app_name: str = "AI Questions Generator"
    openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    moderation_model: str = os.getenv("OPENAI_MODERATION_MODEL", "omni-moderation-latest")
    api_key: str | None = os.getenv("API_KEY")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()

