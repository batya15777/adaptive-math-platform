from __future__ import annotations

from .models import LanguageCode

EMPTY_INPUT_MESSAGES = {
    LanguageCode.en: "Input cannot be empty. Please ask a question.",
    LanguageCode.he: "הקלט לא יכול להיות ריק. אנא כתבו שאלה.",
    LanguageCode.ru: "Ввод не может быть пустым. Пожалуйста, задайте вопрос.",
}

SAFETY_MESSAGES = {
    LanguageCode.en: "I cannot process this request due to safety protocols.",
    LanguageCode.he: "איני יכול לעבד את הבקשה הזו עקב נהלי בטיחות.",
    LanguageCode.ru: "Я не могу обработать этот запрос из-за правил безопасности.",
}

EMPTY_RESPONSE_MESSAGES = {
    LanguageCode.en: "System Error: The agent returned an empty response.",
    LanguageCode.he: "שגיאת מערכת: הסוכן החזיר תשובה ריקה.",
    LanguageCode.ru: "Ошибка системы: агент вернул пустой ответ.",
}

QUESTION_TEMPLATES = {
    LanguageCode.en: {
        "arithmetic": "What is {expression}?",
        "geometry": "A square has side length {side}. What is its area?",
    },
    LanguageCode.he: {
        "arithmetic": "מהו {expression}?",
        "geometry": "לריבוע אורך צלע {side}. מה השטח שלו?",
    },
    LanguageCode.ru: {
        "arithmetic": "Сколько будет {expression}?",
        "geometry": "У квадрата сторона {side}. Какова его площадь?",
    },
}

STEP_TEMPLATES = {
    LanguageCode.en: {
        "arithmetic": [
            "Start with {first_number}",
            "Apply the operation step by step",
            "The answer is {answer}",
        ],
        "geometry": [
            "Use the area formula for a square: side × side",
            "Substitute the side length into the formula: {expression}",
            "Compute the area to get {answer}",
        ],
    },
    LanguageCode.he: {
        "arithmetic": [
            "מתחילים ב-{first_number}",
            "מבצעים את הפעולה שלב אחרי שלב",
            "התשובה היא {answer}",
        ],
        "geometry": [
            "משתמשים בנוסחת השטח של ריבוע: צלע × צלע",
            "מציבים את אורך הצלע בנוסחה: {expression}",
            "מחשבים את השטח ומקבלים {answer}",
        ],
    },
    LanguageCode.ru: {
        "arithmetic": [
            "Начинаем с {first_number}",
            "Выполняем действие шаг за шагом",
            "Ответ: {answer}",
        ],
        "geometry": [
            "Используем формулу площади квадрата: сторона × сторона",
            "Подставляем длину стороны в формулу: {expression}",
            "Вычисляем площадь и получаем {answer}",
        ],
    },
}

GEOMETRY_ROUTE_HINTS = {
    LanguageCode.en: "Detected geometry-related topic.",
    LanguageCode.he: "זוהה נושא הקשור לגאומטריה.",
    LanguageCode.ru: "Обнаружена тема, связанная с геометрией.",
}

ARITHMETIC_ROUTE_HINTS = {
    LanguageCode.en: "Detected arithmetic-related topic.",
    LanguageCode.he: "זוהה נושא הקשור לחשבון.",
    LanguageCode.ru: "Обнаружена тема, связанная с арифметикой.",
}

