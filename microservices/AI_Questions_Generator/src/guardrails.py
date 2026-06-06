from __future__ import annotations

import re

from openai import OpenAI

from .models import LanguageCode

FORBIDDEN_KEYWORDS = {
    # 1. Violence, Weapons, and Crime
    "violence": [
        # English
        "gun", "bullet", "shoot", "kill", "murder", "blood", "knife", "sword", "bomb", "explosive", "war", "terror", "kidnap", "hostage", "steal", "rob", "thief", "prison", "jail", "slaughter", "torture", "punch", "beat",
        # Hebrew
        "אקדח", "כדור", "דקירה", "הרג", "רצח", "דם", "חרב", "פצצה", "מלחמה", "אימה", "חטיפה", "בן ערובה", "גנבה", "שודד", "גנב",
        # Russian
        "пистолет", "пуля", "расстрелять", "убить", "убийство", "кровь", "нож", "меч", "бомба", "взрыв", "война", "террор", "похитить", "заложник", "украсть", "грабитель", "вор",
    ],

    # 2. Substances and Paraphernalia
    "substances": [
        # English
        "alcohol", "beer", "wine", "vodka", "liquor", "drunk", "drugs", "weed", "cocaine", "heroin", "smoke", "vape", "cigarette", "cigar", "tobacco", "high", "intoxicated", "pill", "overdose",
        # Hebrew
        "אלכוהול", "בירה", "יין", "וודקה", "שיכרות", "סמים", "סיגריה", "טבק", "גבוה", "שיכור",
        # Russian
        "алкоголь", "пиво", "вино", "водка", "пьяный", "наркотики", "сигарета", "табак", "высокий", "пьяный",
    ],

    # 3. Gambling and Unethical Financial Contexts
    "gambling": [
        # English
        "casino", "gamble", "bet", "betting", "lottery", "poker", "blackjack", "roulette", "slot machine", "wager", "bribe", "scam",
        # Hebrew
        "קזינו", "הימור", "הימורים", "הגרלה", "שוחד", "הונאה",
        # Russian
        "казино", "азартная игра", "ставка", "лотерея", "покер", "взятка", "мошенничество",
    ],

    # 4. Scary, Self-Harm, or Dark Themes
    "dark_themes": [
        # English
        "suicide", "depressed", "starve", "cut", "death", "die", "dead", "corpse", "grave", "cemetery", "demon", "devil", "hell",
        # Hebrew
        "התאבדות", "עצוב", "מת", "מוות", "גופה", "קברו", "בית קברים", "שד", "שטן", "גיהנום",
        # Russian
        "самоубийство", "депрессия", "умереть", "смерть", "мертвец", "демон", "дьявол", "ад",
    ],

    # 5. Adult Content & Profanity
    "adult_content": [
        # English
        "sex", "porn", "naked", "nude", "stripper", "prostitute", "dating", "kiss", "damn", "crap", "bastard",
        # Hebrew
        "סקס", "פורנו", "עירום", "זונה", "קללה",
        # Russian
        "секс", "порно", "голый", "проститутка", "проклятие",
    ],

    # 6. Political / forbidden operational terms
    "policy": ["פוליטיקה", "בחירות", "שמאל", "ימין", "rm -rf", "drop table", "hack", "bypass"],
}

FLAT_FORBIDDEN_KEYWORDS = [word for category in FORBIDDEN_KEYWORDS.values() for word in category]

LANGUAGE_MESSAGES = {
    LanguageCode.en: "I cannot process this request due to safety protocols.",
    LanguageCode.he: "איני יכול לעבד את הבקשה הזו עקב נהלי בטיחות.",
    LanguageCode.ru: "Я не могу обработать этот запрос из-за правил безопасности.",
}

EMPTY_INPUT_MESSAGES = {
    LanguageCode.en: "Input cannot be empty. Please ask a question.",
    LanguageCode.he: "הקלט לא יכול להיות ריק. אנא כתבו שאלה.",
    LanguageCode.ru: "Ввод не может быть пустым. Пожалуйста, задайте вопрос.",
}

EMPTY_OUTPUT_MESSAGES = {
    LanguageCode.en: "System Error: The agent returned an empty response.",
    LanguageCode.he: "שגיאת מערכת: הסוכן החזיר תשובה ריקה.",
    LanguageCode.ru: "Ошибка системы: агент вернул пустой ответ.",
}


def _strip_hebrew_prefixes(text: str) -> str:
    """Generate loose Hebrew prefix variants for matching."""
    tokens = re.split(r"(\W+)", text)
    variants = [text]
    for token in tokens:
        if token.isalpha() and re.search(r"[\u0590-\u05FF]", token):
            variants.extend([f"{prefix}{token}" for prefix in ["ב", "ל", "ה", "ו", "מ", "כ", "ש", "כּ", "לְ", "מִ"]])
    return " ".join(dict.fromkeys(variants))


def _contains_forbidden(text: str) -> bool:
    normalized = text.lower()
    hebrew_variant_source = _strip_hebrew_prefixes(text)
    searchable_text = f"{normalized} {hebrew_variant_source.lower()}"
    for keyword in FLAT_FORBIDDEN_KEYWORDS:
        pattern = r'\b' + re.escape(keyword.lower()) + r'\b'
        if re.search(pattern, searchable_text):
            return True
    return False

def check_input_guardrails(user_input: str) -> str | None:
    """
    Input Guardrails (Part F)
    Checks the input BEFORE it reaches the Router.
    Returns an error message if blocked, or None if safe to proceed.
    """
    if not user_input or not user_input.strip():
        return EMPTY_INPUT_MESSAGES[LanguageCode.en]

    if _contains_forbidden(user_input):
        return LANGUAGE_MESSAGES[LanguageCode.en]

    return None


def check_output_guardrails(final_output: str) -> str:
    """
    Output Guardrails (Part G)
    Checks the final text output BEFORE showing it to the user.
    """
    if _contains_forbidden(final_output):
        return LANGUAGE_MESSAGES[LanguageCode.en]

    if not final_output or not final_output.strip():
        return EMPTY_OUTPUT_MESSAGES[LanguageCode.en]

    return final_output


class ModerationGuardrail:
    """OpenAI moderation-based input guardrail."""

    def __init__(self, client: OpenAI):
        self._client = client

    def check(self, text: str, language: LanguageCode = LanguageCode.en) -> str | None:
        local = check_input_guardrails(text)
        if local:
            return LANGUAGE_MESSAGES.get(language, local) if local == LANGUAGE_MESSAGES[LanguageCode.en] else local

        response = self._client.moderations.create(input=text)
        if response.results and response.results[0].flagged:
            return LANGUAGE_MESSAGES[language]

        return None
