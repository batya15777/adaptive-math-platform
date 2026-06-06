from __future__ import annotations

import re

from openai import OpenAI

from .i18n import EMPTY_INPUT_MESSAGES, SAFETY_MESSAGES
from .models import LanguageCode

# ---------------------------------------------------------------------------
# Keyword blocklist — 6 categories, 3 languages.
# Uses word-boundary regex matching to prevent false positives
# (e.g. "rob" inside "problem", "die" inside "studies").
# ---------------------------------------------------------------------------

FORBIDDEN_KEYWORDS: dict[str, list[str]] = {
    # 1. Violence, Weapons, Crime
    "violence": [
        # English
        "gun", "bullet", "shoot", "kill", "murder", "blood", "knife", "sword",
        "bomb", "explosive", "war", "terror", "kidnap", "hostage", "steal",
        "rob", "thief", "prison", "jail", "slaughter", "torture", "punch", "beat",
        # Hebrew
        "אקדח", "כדור", "דקירה", "הרג", "רצח", "דם", "חרב", "פצצה",
        "מלחמה", "אימה", "חטיפה", "בן ערובה", "גנבה", "שודד", "גנב",
        # Russian
        "пистолет", "пуля", "расстрелять", "убить", "убийство", "кровь",
        "нож", "меч", "бомба", "взрыв", "война", "террор", "похитить",
        "заложник", "украсть", "грабитель", "вор",
    ],

    # 2. Substances and Paraphernalia
    "substances": [
        # English
        "alcohol", "beer", "wine", "vodka", "liquor", "drunk", "drugs",
        "weed", "cocaine", "heroin", "smoke", "vape", "cigarette", "cigar",
        "tobacco", "intoxicated", "pill", "overdose",
        # Hebrew
        "אלכוהול", "בירה", "יין", "וודקה", "שיכרות", "סמים",
        "סיגריה", "טבק", "שיכור",
        # Russian
        "алкоголь", "пиво", "вино", "водка", "пьяный", "наркотики",
        "сигарета", "табак",
    ],

    # 3. Gambling and Unethical Finance
    "gambling": [
        # English
        "casino", "gamble", "bet", "betting", "lottery", "poker",
        "blackjack", "roulette", "wager", "bribe", "scam",
        # Hebrew
        "קזינו", "הימור", "הימורים", "הגרלה", "שוחד", "הונאה",
        # Russian
        "казино", "азартная игра", "ставка", "лотерея", "покер",
        "взятка", "мошенничество",
    ],

    # 4. Dark Themes and Self-Harm
    "dark_themes": [
        # English
        "suicide", "depressed", "starve", "death", "corpse",
        "grave", "cemetery", "demon", "devil", "hell",
        # Hebrew
        "התאבדות", "מת", "מוות", "גופה", "קבר", "בית קברות",
        "שד", "שטן", "גיהנום",
        # Russian
        "самоубийство", "депрессия", "умереть", "смерть",
        "мертвец", "демон", "дьявол", "ад",
    ],

    # 5. Adult Content and Profanity
    "adult_content": [
        # English
        "sex", "porn", "naked", "nude", "stripper", "prostitute",
        "damn", "crap", "bastard",
        # Hebrew
        "סקס", "פורנו", "עירום", "זונה", "קללה",
        # Russian
        "секс", "порно", "голый", "проститутка", "проклятие",
    ],

    # 6. Policy / Malicious Injection
    "policy": [
        "פוליטיקה", "בחירות", "rm -rf", "drop table", "hack", "bypass",
    ],
}

_FLAT_KEYWORDS: list[str] = [
    word for category in FORBIDDEN_KEYWORDS.values() for word in category
]


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _strip_hebrew_prefixes(text: str) -> str:
    """
    Generate loose Hebrew prefix variants to catch prefixed forms of keywords
    (e.g. "בסמים" = "in drugs", which should still match "סמים").
    """
    tokens = re.split(r"(\W+)", text)
    variants = [text]
    for token in tokens:
        if token.isalpha() and re.search(r"[\u0590-\u05FF]", token):
            variants.extend(
                f"{prefix}{token}"
                for prefix in ["ב", "ל", "ה", "ו", "מ", "כ", "ש", "כּ", "לְ", "מִ"]
            )
    return " ".join(dict.fromkeys(variants))


def _contains_forbidden(text: str) -> bool:
    normalized = text.lower()
    searchable = f"{normalized} {_strip_hebrew_prefixes(text).lower()}"
    for keyword in _FLAT_KEYWORDS:
        pattern = r"\b" + re.escape(keyword.lower()) + r"\b"
        if re.search(pattern, searchable):
            return True
    return False


# ---------------------------------------------------------------------------
# Public guardrail functions
# ---------------------------------------------------------------------------

def check_input_guardrails(user_input: str, language: LanguageCode = LanguageCode.en) -> str | None:
    """
    Validate input before it reaches the LLM.
    Returns a localised error message string if blocked, or ``None`` if safe.
    """
    if not user_input or not user_input.strip():
        return EMPTY_INPUT_MESSAGES[language]

    if _contains_forbidden(user_input):
        return SAFETY_MESSAGES[language]

    return None


def check_output_guardrails(final_output: str) -> str:
    """
    Final safety pass on generated text before it reaches the caller.
    Returns the original text if clean, or a safe error string if blocked.
    """
    if not final_output or not final_output.strip():
        return "System Error: The agent returned an empty response."

    if _contains_forbidden(final_output):
        return "I cannot process this request due to safety protocols"

    return final_output


# ---------------------------------------------------------------------------
# OpenAI Moderation guardrail
# ---------------------------------------------------------------------------

class ModerationGuardrail:
    """
    Two-stage input guardrail:
    1. Local keyword check (fast, offline).
    2. OpenAI Moderation API (catches things the keyword list misses).
    """

    def __init__(self, client: OpenAI) -> None:
        self._client = client

    def check(self, text: str, language: LanguageCode = LanguageCode.en) -> str | None:
        # Stage 1 — local keyword filter
        local_block = check_input_guardrails(text, language)
        if local_block:
            return local_block

        # Stage 2 — OpenAI moderation
        response = self._client.moderations.create(input=text)
        if response.results and response.results[0].flagged:
            return SAFETY_MESSAGES[language]

        return None
