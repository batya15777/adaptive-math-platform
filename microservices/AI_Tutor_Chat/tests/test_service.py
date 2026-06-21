from src.models import TutorAction, TutorChatRequest
from src.provider import ProviderReply
from src.service import TutorService

from test_api import request_body


class StubProvider:
    def __init__(self, reply: ProviderReply | None = None, should_fail: bool = False):
        self._reply = reply
        self._should_fail = should_fail

    def reply(self, request: TutorChatRequest) -> ProviderReply:
        if self._should_fail:
            raise RuntimeError("provider unavailable")
        assert self._reply is not None
        return self._reply


def build_request(guidance_level: int = 0) -> TutorChatRequest:
    return TutorChatRequest.model_validate(request_body(guidance_level=guidance_level))


def test_provider_response_is_used() -> None:
    service = TutorService(
        provider=StubProvider(
            ProviderReply(
                message="נסי לפרק את התרגיל לשני חלקים.",
                action=TutorAction.hint,
            )
        )
    )

    response = service.reply(build_request())

    assert response.message == "נסי לפרק את התרגיל לשני חלקים."
    assert response.guidance_level == 1


def test_provider_answer_is_replaced_with_natural_local_hint() -> None:
    service = TutorService(
        provider=StubProvider(
            ProviderReply(
                message="התשובה היא 15.",
                action=TutorAction.hint,
            )
        )
    )

    response = service.reply(build_request())

    assert "15" not in response.message
    assert "[התשובה מוסתרת]" not in response.message
    assert "נתונים" in response.message


def test_provider_failure_uses_local_hint() -> None:
    service = TutorService(provider=StubProvider(should_fail=True))

    response = service.reply(build_request())

    assert response.action == TutorAction.hint
    assert "נתונים" in response.message


def test_correct_final_answer_is_confirmed_even_when_it_echoes_answer() -> None:
    # When the student states the correct final answer, the tutor may confirm it
    # (echoing the answer back is praise, not a leak) instead of falling back.
    service = TutorService(
        provider=StubProvider(
            ProviderReply(
                message="כל הכבוד! הגעת ל-15, סיימת את התרגיל!",
                action=TutorAction.hint,
            )
        )
    )

    request = TutorChatRequest.model_validate(
        request_body(message="15", guidance_level=4)
    )
    response = service.reply(request)

    assert response.action == TutorAction.hint
    assert "15" in response.message


def test_redirect_does_not_advance_guidance_level() -> None:
    service = TutorService(
        provider=StubProvider(
            ProviderReply(
                message="בואי נחזור לתרגיל.",
                action=TutorAction.redirect,
            )
        )
    )

    response = service.reply(build_request(guidance_level=2))

    assert response.action == TutorAction.redirect
    assert response.guidance_level == 2


def test_later_hint_with_answer_falls_back_to_safe_step() -> None:
    # The provider hint contains the final answer, so the answer-leak guard must
    # fall back to a local hint that does not reveal it.
    service = TutorService(
        provider=StubProvider(
            ProviderReply(
                message="מה צריך להוסיף ל-10 כדי להגיע ל-15?",
                action=TutorAction.hint,
            )
        )
    )

    response = service.reply(build_request(guidance_level=2))

    assert response.action == TutorAction.hint
    assert response.guidance_level == 3
    assert "15" not in response.message
    assert "[התשובה מוסתרת]" not in response.message
    assert response.message.strip() != ""
