from __future__ import annotations

from .models import LanguageCode

# ---------------------------------------------------------------------------
# Cheap, deterministic language guard (no extra LLM call).
#
# The generator LLM occasionally ignores the requested output language and
# answers in English. Such a question would be cached tagged with the requested
# language and then served to users of that language forever. We catch the gross
# case by checking which script the text actually uses:
#   - Hebrew  → U+0590..U+05FF
#   - Cyrillic→ U+0400..U+04FF
#
# The check for he/ru is "contains the expected script" rather than "contains
# only" on purpose: the literal placeholder word NAME is always English by design
# (see GENERATOR_SYSTEM_PROMPT rule 4), and digits/math symbols are script-neutral.
# For English we instead require the absence of Hebrew/Cyrillic.
# ---------------------------------------------------------------------------

_HEBREW_START, _HEBREW_END = "֐", "׿"
_CYRILLIC_START, _CYRILLIC_END = "Ѐ", "ӿ"


def _has_hebrew(text: str) -> bool:
    return any(_HEBREW_START <= c <= _HEBREW_END for c in text)


def _has_cyrillic(text: str) -> bool:
    return any(_CYRILLIC_START <= c <= _CYRILLIC_END for c in text)


def language_mismatch(text: str, expected: LanguageCode) -> str | None:
    """Return a feedback string if `text` is clearly not in `expected`, else None."""
    has_hebrew = _has_hebrew(text)
    has_cyrillic = _has_cyrillic(text)

    if expected is LanguageCode.he and not has_hebrew:
        return "Question text is not in Hebrew (no Hebrew characters found)."
    if expected is LanguageCode.ru and not has_cyrillic:
        return "Question text is not in Russian (no Cyrillic characters found)."
    if expected is LanguageCode.en and (has_hebrew or has_cyrillic):
        return "Question text is not in English (contains non-Latin script)."
    return None
