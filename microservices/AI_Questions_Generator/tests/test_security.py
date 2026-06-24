"""Tests for the X-API-Key gate. The dependency is open when no key is configured
(dev mode) and strict when a key is set — a security boundary worth pinning down.
``get_settings`` is monkeypatched so the test never depends on the real environment.
"""
from __future__ import annotations

from types import SimpleNamespace

import pytest
from fastapi import HTTPException

from src import security


def _settings(api_key: str | None) -> SimpleNamespace:
    return SimpleNamespace(api_key=api_key)


class TestVerifyApiKey:
    def test_no_key_configured_allows_any_request(self, monkeypatch):
        monkeypatch.setattr(security, "get_settings", lambda: _settings(None))
        assert security.verify_api_key("anything") is None
        assert security.verify_api_key(None) is None

    def test_correct_key_passes(self, monkeypatch):
        monkeypatch.setattr(security, "get_settings", lambda: _settings("secret"))
        assert security.verify_api_key("secret") is None

    def test_wrong_key_is_forbidden(self, monkeypatch):
        monkeypatch.setattr(security, "get_settings", lambda: _settings("secret"))
        with pytest.raises(HTTPException) as exc:
            security.verify_api_key("wrong-key")
        assert exc.value.status_code == 403

    def test_missing_key_when_required_is_forbidden(self, monkeypatch):
        monkeypatch.setattr(security, "get_settings", lambda: _settings("secret"))
        with pytest.raises(HTTPException) as exc:
            security.verify_api_key(None)
        assert exc.value.status_code == 403
