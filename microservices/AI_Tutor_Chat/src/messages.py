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

EMPATHY_PREFIXES = {
    LanguageCode.he: "זה ממש בסדר להתקשות, ואני כאן כדי לעזור לך. ",
    LanguageCode.en: "It's completely okay to find this hard, and I'm here to help you. ",
    LanguageCode.ru: "Это совершенно нормально, что трудно, и я рядом, чтобы помочь. ",
}

WRONG_ANSWER_MESSAGES = {
    LanguageCode.he: "לא בדיוק, אבל יופי שניסית! בואי נבדוק שוב צעד קטן — מה קיבלת בשלב הראשון?",
    LanguageCode.en: "Not quite, but great that you tried! Let's check one small step again — what did you get in the first step?",
    LanguageCode.ru: "Не совсем, но молодец, что попробовала! Давай проверим один маленький шаг — что получилось на первом шаге?",
}

CORRECT_ANSWER_MESSAGES = {
    LanguageCode.he: "נכון, כל הכבוד! אפשר להמשיך.",
    LanguageCode.en: "Correct, well done! We can continue.",
    LanguageCode.ru: "Верно, молодец! Можно продолжать.",
}

INTERMEDIATE_CORRECT_MESSAGES = {
    LanguageCode.he: "נכון, זה צעד טוב! עכשיו נמשיך בזהירות לצעד הבא.",
    LanguageCode.en: "Correct, that's a good step! Now let's carefully continue to the next step.",
    LanguageCode.ru: "Верно, это хороший шаг! Теперь аккуратно перейдём к следующему шагу.",
}

DIRECT_ANSWER_REFUSALS = {
    LanguageCode.he: "אני יודעת שמתחשק לך לקבל את התשובה, אבל אני לא אגלה אותה — ביחד נגיע אליה! בואי נתחיל מצעד קטן: מה המספרים בתרגיל ומה צריך לעשות איתם?",
    LanguageCode.en: "I know you'd love to get the answer, but I won't reveal it — we'll get there together! Let's start with a small step: what are the numbers in the exercise and what should we do with them?",
    LanguageCode.ru: "Я понимаю, что хочется получить ответ, но я его не раскрою — дойдём вместе! Начнём с маленького шага: какие числа в задаче и что с ними нужно сделать?",
}

STUCK_HINTS = {
    LanguageCode.he: [
        "בואי נפרק את התרגיל לחלקים קטנים. מה המספר הראשון, ומה צריך לעשות איתו?",
        "נסי לדמיין את המספרים. מה קורה אם מתחילים דווקא מהמספר הגדול?",
        "אין לחץ! מה כבר הצלחת לחשב, ומה החלק שעדיין נשאר?",
    ],
    LanguageCode.en: [
        "Let's break the exercise into small parts. What is the first number, and what should we do with it?",
        "Try to picture the numbers. What happens if you start from the bigger number?",
        "No pressure! What did you already manage to compute, and which part is still left?",
    ],
    LanguageCode.ru: [
        "Давай разобьём задачу на маленькие части. Какое первое число и что с ним делать?",
        "Попробуй представить числа. Что будет, если начать с большего числа?",
        "Без спешки! Что ты уже посчитала, а какая часть осталась?",
    ],
}
