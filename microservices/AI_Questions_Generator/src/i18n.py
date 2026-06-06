from __future__ import annotations

from .models import LanguageCode

# ---------------------------------------------------------------------------
# Human-readable language names — used in LLM prompts so the model knows
# exactly which language to write in.
# ---------------------------------------------------------------------------

LANGUAGE_NAMES: dict[LanguageCode, str] = {
    LanguageCode.en: "English",
    LanguageCode.he: "Hebrew (עברית)",
    LanguageCode.ru: "Russian (Русский)",
}

# ---------------------------------------------------------------------------
# Error / safety messages — one per supported language.
# ---------------------------------------------------------------------------

EMPTY_INPUT_MESSAGES: dict[LanguageCode, str] = {
    LanguageCode.en: "Input cannot be empty. Please provide a topic and theme.",
    LanguageCode.he: "הקלט לא יכול להיות ריק. אנא ספקו נושא ותמה.",
    LanguageCode.ru: "Ввод не может быть пустым. Укажите тему и сюжет.",
}

SAFETY_MESSAGES: dict[LanguageCode, str] = {
    LanguageCode.en: "I cannot process this request due to safety protocols.",
    LanguageCode.he: "איני יכול לעבד את הבקשה הזו עקב נהלי בטיחות.",
    LanguageCode.ru: "Я не могу обработать этот запрос из-за правил безопасности.",
}

EMPTY_RESPONSE_MESSAGES: dict[LanguageCode, str] = {
    LanguageCode.en: "System Error: The agent returned an empty response.",
    LanguageCode.he: "שגיאת מערכת: הסוכן החזיר תשובה ריקה.",
    LanguageCode.ru: "Ошибка системы: агент вернул пустой ответ.",
}
