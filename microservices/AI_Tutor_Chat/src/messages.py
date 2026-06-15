from __future__ import annotations

from .models import LanguageCode


SAFETY_MESSAGES = {
    LanguageCode.he: "אני כאן כדי לעזור בלמידה בצורה בטוחה. בואי נחזור לתרגיל.",
    LanguageCode.en: "I am here to help with learning safely. Let us return to the exercise.",
    LanguageCode.ru: "Я здесь, чтобы безопасно помогать с учебой. Вернемся к заданию.",
}

OFF_TOPIC_MESSAGES = {
    LanguageCode.he: "זה נושא מעניין, אבל כאן נתמקד בתרגיל. מה בתרגיל לא ברור לך?",
    LanguageCode.en: "That is an interesting topic, but let us focus on the exercise. Which part is unclear?",
    LanguageCode.ru: "Это интересная тема, но давай сосредоточимся на задании. Какая часть непонятна?",
}

START_HINTS = {
    LanguageCode.he: "בואי נתחיל בצעד קטן: אילו נתונים חשובים מופיעים בשאלה?",
    LanguageCode.en: "Let us start with a small step: which important values appear in the question?",
    LanguageCode.ru: "Начнем с небольшого шага: какие важные данные есть в задаче?",
}

STEP_PREFIXES = {
    LanguageCode.he: "רמז לצעד הבא: ",
    LanguageCode.en: "Hint for the next step: ",
    LanguageCode.ru: "Подсказка к следующему шагу: ",
}

CONTINUE_PROMPTS = {
    LanguageCode.he: " נסי לבצע את הצעד הזה ולספר לי מה קיבלת.",
    LanguageCode.en: " Try this step and tell me what you get.",
    LanguageCode.ru: " Попробуй выполнить этот шаг и скажи, что получилось.",
}

ANSWER_FREE_HINTS = {
    LanguageCode.he: "חשבי על מה שכבר חישבת ועל החלק שעדיין נשאר לבצע. מהו הצעד הקטן הבא?",
    LanguageCode.en: "Think about what you already calculated and the part still left to do. What is the next small step?",
    LanguageCode.ru: "Подумай, что уже вычислено и какую часть еще нужно выполнить. Какой следующий маленький шаг?",
}
