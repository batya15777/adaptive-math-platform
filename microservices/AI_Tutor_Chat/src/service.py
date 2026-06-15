from __future__ import annotations

from .guardrails import (
    contains_answer,
    is_clearly_off_topic,
    is_safe_output,
    is_unsafe,
)
from .messages import (
    ANSWER_FREE_HINTS,
    CONTINUE_PROMPTS,
    OFF_TOPIC_MESSAGES,
    SAFETY_MESSAGES,
    START_HINTS,
    STEP_PREFIXES,
)
from .models import TutorAction, TutorChatRequest, TutorChatResponse
from .provider import TutorProvider


class TutorService:
    """Coordinates guardrails, the AI provider, and the local fallback tutor."""

    def __init__(self, provider: TutorProvider | None = None) -> None:
        self._provider = provider

    def reply(self, request: TutorChatRequest) -> TutorChatResponse:
        language = request.question.language

        if is_unsafe(request.student_message):
            return TutorChatResponse(
                message=SAFETY_MESSAGES[language],
                guidance_level=request.guidance_level,
                action=TutorAction.safety,
            )

        if is_clearly_off_topic(request.student_message):
            return TutorChatResponse(
                message=OFF_TOPIC_MESSAGES[language],
                guidance_level=request.guidance_level,
                action=TutorAction.redirect,
            )

        if self._provider:
            try:
                provider_reply = self._provider.reply(request)
                if contains_answer(provider_reply.message, request.question):
                    if provider_reply.action == TutorAction.hint:
                        return self._local_hint(request)
                    if provider_reply.action == TutorAction.redirect:
                        return TutorChatResponse(
                            message=OFF_TOPIC_MESSAGES[language],
                            guidance_level=request.guidance_level,
                            action=TutorAction.redirect,
                        )
                    return TutorChatResponse(
                        message=SAFETY_MESSAGES[language],
                        guidance_level=request.guidance_level,
                        action=TutorAction.safety,
                    )

                if is_safe_output(provider_reply.message):
                    next_level = request.guidance_level
                    if provider_reply.action == TutorAction.hint:
                        next_level = min(request.guidance_level + 1, 5)
                    return TutorChatResponse(
                        message=provider_reply.message,
                        guidance_level=next_level,
                        action=provider_reply.action,
                    )
            except Exception:
                # A provider failure must not prevent the student from receiving help.
                pass

        return self._local_hint(request)

    @staticmethod
    def _local_hint(request: TutorChatRequest) -> TutorChatResponse:
        language = request.question.language
        next_level = min(request.guidance_level + 1, 5)
        if request.guidance_level == 0:
            message = START_HINTS[language]
        else:
            step_index = min(
                request.guidance_level - 1,
                len(request.question.solution_steps) - 1,
            )
            step = request.question.solution_steps[step_index]
            if contains_answer(step, request.question):
                message = ANSWER_FREE_HINTS[language]
            else:
                message = STEP_PREFIXES[language] + step + CONTINUE_PROMPTS[language]

        return TutorChatResponse(
            message=message,
            guidance_level=next_level,
            action=TutorAction.hint,
        )
