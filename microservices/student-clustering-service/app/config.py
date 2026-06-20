from __future__ import annotations

import os
from functools import lru_cache


class Settings:
    """
    Runtime configuration, overridable via environment variables.

    Mirrors the simple settings pattern used by the AI_Questions_Generator service
    so both microservices feel the same to operate.
    """

    app_name: str = "Student Clustering Service"

    # Server — note port 8001: the AI question generator already owns 8000.
    host: str = os.getenv("CLUSTERING_HOST", "127.0.0.1")
    port: int = int(os.getenv("CLUSTERING_PORT", "8001"))

    # K-Means tuning
    random_state: int = int(os.getenv("KMEANS_RANDOM_STATE", "42"))
    # Upper bound for the automatic k search (silhouette based).
    max_k: int = int(os.getenv("KMEANS_MAX_K", "8"))


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
