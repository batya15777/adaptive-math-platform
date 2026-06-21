from __future__ import annotations

from fastapi import Depends, HTTPException
from fastapi.security import APIKeyHeader

from .settings import get_settings

_API_KEY_HEADER = APIKeyHeader(name="X-API-Key", auto_error=False)


def verify_api_key(x_api_key: str | None = Depends(_API_KEY_HEADER)) -> None:
    configured_key = get_settings().api_key
    if configured_key is None:
        return  # No key configured — open in development mode
    if x_api_key != configured_key:
        raise HTTPException(status_code=403, detail="Forbidden: invalid or missing X-API-Key header.")
