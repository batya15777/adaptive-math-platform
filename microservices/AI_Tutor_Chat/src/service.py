from __future__ import annotations

import logging

from .guardrails import contains_answer, is_safe_output, is_unsafe
from .messages import (
    CONTINUE_PROMPTS,
    SAFETY_MESSAGES,
    START_HINTS,
    STEP_PREFIXES,
    STUCK_HINTS,
)
from .models import TutorAction, TutorChatRequest, TutorChatResponse
from .provider import TutorProvider


logger = logging.getLogger(__name__)


class TutorService:
    """Coordinates the safety guards, the AI provider, and the local fallback tutor.

    Generic classification and tutoring (off-topic, inappropriate, answer checking,
    intermediate steps, frustration, etc.) are handled by the provider so the tutor
    works for any exercise type. The service only enforces hard safety guards and a
    local fallback when the provider is unavailable."""

    def __init__(self, provider: TutorProvider | None = None) -> None:
        self._provider = provider

    def reply(self, request: TutorChatRequest) -> TutorChatResponse:
        language = request.question.language

        # Hard safety pre-filter for clearly dangerous content.
        if is_unsafe(request.student_message):
            return TutorChatResponse(
                message=SAFETY_MESSAGES[language],
                guidance_level=request.guidance_level,
                action=TutorAction.safety,
            )

        if self._provider:
            try:
                provider_reply = self._provider.reply(request)

                # Answer-leak safety net: never pass a reply that exposes the answer.
                # Exception: when the student has already stated the correct answer
                # themselves, the tutor is confirming it (not revealing it), so
                # echoing the answer back to praise them is allowed.
                student_stated_answer = contains_answer(
                    request.student_message, request.question
                )
                if not student_stated_answer and contains_answer(
                    provider_reply.message, request.question
                ):
                    return self._local_hint(request)

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
                logger.exception("OpenAI tutor provider failed; falling back to local hint.")

        return self._local_hint(request)

    @staticmethod
    def _local_hint(request: TutorChatRequest) -> TutorChatResponse:
        """Safe degraded-mode hint used only when the provider is unavailable or
        leaked the answer. Never reveals the final answer."""
        language = request.question.language
        next_level = min(request.guidance_level + 1, 5)
        if request.guidance_level == 0:
            message = START_HINTS[language]
        else:
            step_index = min(
                request.guidance_level - 1,
                len(request.question.solution_steps) - 1,
            )
            # Walk backward to the latest step that does not contain the answer.
            safe_step = None
            while step_index >= 0:
                candidate = request.question.solution_steps[step_index]
                if not contains_answer(candidate, request.question):
                    safe_step = candidate
                    break
                step_index -= 1

            if safe_step is not None:
                message = STEP_PREFIXES[language] + safe_step + CONTINUE_PROMPTS[language]
            else:
                # No answer-free step exists: rotate safe generic hints so a stuck
                # student does not see the exact same sentence every time.
                options = STUCK_HINTS[language]
                message = options[request.guidance_level % len(options)]

        return TutorChatResponse(
            message=message,
            guidance_level=next_level,
            action=TutorAction.hint,
        )
