import json
from types import SimpleNamespace

from src.models import TutorAction, TutorChatRequest
from src.provider import OpenAITutorProvider

from test_api import request_body


class StubCompletions:
    def __init__(self, payload: dict):
        self._payload = payload

    def create(self, **kwargs):
        content = json.dumps(self._payload, ensure_ascii=False)
        message = SimpleNamespace(content=content)
        choice = SimpleNamespace(message=message)
        return SimpleNamespace(choices=[choice])


class StubClient:
    def __init__(self, payload: dict):
        self.chat = SimpleNamespace(completions=StubCompletions(payload))


def build_provider(payload: dict) -> OpenAITutorProvider:
    return OpenAITutorProvider(client=StubClient(payload), model="test-model")


def build_request() -> TutorChatRequest:
    return TutorChatRequest.model_validate(request_body())


def test_provider_classifies_unrelated_message_as_redirect() -> None:
    provider = build_provider(
        {
            "message": "בואי נחזור לתרגיל.",
            "is_related_to_exercise": False,
            "is_safe": True,
        }
    )

    response = provider.reply(build_request())

    assert response.action == TutorAction.redirect


def test_provider_classifies_unsafe_message_as_safety() -> None:
    provider = build_provider(
        {
            "message": "בואי נחזור ללמידה בטוחה.",
            "is_related_to_exercise": False,
            "is_safe": False,
        }
    )

    response = provider.reply(build_request())

    assert response.action == TutorAction.safety
