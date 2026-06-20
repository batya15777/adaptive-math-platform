from __future__ import annotations

import re

from .models import LanguageCode, QuestionContext


UNSAFE_TERMS = {
    "kill",
    "murder",
    "suicide",
    "porn",
    "סקס",
    "פורנו",
    "התאבדות",
    "רצח",
    "убить",
    "самоубийство",
}

OFF_TOPIC_TERMS = {
    "football",
    "soccer",
    "celebrity",
    "singer",
    "movie",
    "כדורגל",
    "זמר",
    "זמרת",
    "סרט",
    "футбол",
    "певец",
    "фильм",
}


EMOTIONAL_TERMS = {
    "טיפש",
    "טיפשה",
    "מטומטם",
    "מטומטמת",
    "לא מבין",
    "לא מבינה",
    "לא יודע",
    "לא יודעת",
    "קשה לי",
    "אוף",
    "לא רגיש",
    "מתוסכל",
    "מתוסכלת",
    "מאוכזב",
    "מאוכזבת",
    "נמאס",
    "נמאס לי",
    "stupid",
    "dumb",
}


DIRECT_ANSWER_TERMS = {
    "תגלה לי",
    "תגלי לי",
    "תן לי את התשובה",
    "תני לי את התשובה",
    "מה התשובה",
    "מהי התשובה",
    "מה הפתרון",
    "פתור לי",
    "פתרי לי",
    "תפתור לי",
    "תפתרי לי",
    "תגיד לי כמה זה",
    "תגידי לי כמה זה",
    "give me the answer",
    "what is the answer",
    "tell me the answer",
    "solve it for me",
}


STUCK_TERMS = {
    "לא יודע",
    "לא יודעת",
    "לא מבין",
    "לא מבינה",
    "לא הבנתי",
    "קשה לי",
    "אין לי מושג",
    "i don't know",
    "i'm stuck",
}


def contains_term(text: str, terms: set[str]) -> bool:
    normalized = text.casefold()
    for term in terms:
        normalized_term = term.casefold()
        if re.search(r"[\u0590-\u05FF]", normalized_term):
            pattern = rf"(?<![\u0590-\u05FF])[בלוהמשכ]?{re.escape(normalized_term)}(?![\u0590-\u05FF])"
        else:
            pattern = rf"(?<!\w){re.escape(normalized_term)}(?!\w)"

        if re.search(pattern, normalized):
            return True

    return False


def is_unsafe(text: str) -> bool:
    return contains_term(text, UNSAFE_TERMS)


def is_clearly_off_topic(text: str) -> bool:
    return contains_term(text, OFF_TOPIC_TERMS)


def is_emotional(text: str) -> bool:
    return contains_term(text, EMOTIONAL_TERMS)


def is_direct_answer_request(text: str) -> bool:
    return contains_term(text, DIRECT_ANSWER_TERMS)


def is_stuck(text: str) -> bool:
    return contains_term(text, STUCK_TERMS)


def is_safe_output(text: str) -> bool:
    return bool(text.strip()) and not is_unsafe(text)


def contains_answer(text: str, question: QuestionContext) -> bool:
    answer = question.correct_answer.strip()
    if not answer:
        return False

    return re.search(re.escape(answer), text, flags=re.IGNORECASE) is not None


def hide_answer(text: str, question: QuestionContext) -> str:
    answer = question.correct_answer.strip()
    if not answer:
        return text

    replacement = {
        LanguageCode.he: "[התשובה מוסתרת]",
        LanguageCode.en: "[answer hidden]",
        LanguageCode.ru: "[ответ скрыт]",
    }[question.language]

    return re.sub(re.escape(answer), replacement, text, flags=re.IGNORECASE)
