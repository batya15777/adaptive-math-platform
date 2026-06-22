import { getStrings } from "../../i18n/languages.js";

// Daily-practice-specific strings. The in-question UI (Submit / difficulty / "Correct" /
// "try again" …) is reused from questionGameStrings, so it is NOT duplicated here.
// Gender-dependent keys are { male, female, neutral } objects (picked via a safe helper).
export const DAILY_STRINGS = {
    en: {
        title: "Daily practice",
        subtitle: "A short mix of {goal} questions at your level — keep your streak going!",
        progress: "{done}/{goal} correct",
        loading: "Preparing your daily practice…",
        loadError: "Couldn't load the daily practice.",
        empty: "Start practicing a topic first, then your daily practice will open here.",
        next: "Next question →",
        finishEarlyHint: "Answer {goal} correctly to finish",
        back: "Back",
        backHome: "Back to home",
        // completion
        doneTitle: "All done! 🎉",
        donePraise: { male: "Amazing work — you nailed your daily practice!", female: "Amazing work — you nailed your daily practice!", neutral: "Amazing work — you nailed your daily practice!" },
        doneBonus: "+{bonus} bonus points",
        doneStars: "You now have {stars} stars ⭐",
        alreadyDone: "You've already finished today's practice — see you tomorrow! 🌟",
    },
    he: {
        title: "תרגול יומי",
        subtitle: "מיקס קצר של {goal} שאלות ברמה שלך — שמרו על הרצף!",
        progress: "{done}/{goal} נכונות",
        loading: "מכינים לך את התרגול היומי…",
        loadError: "לא ניתן לטעון את התרגול היומי.",
        empty: "התחילו לתרגל נושא, ואז התרגול היומי ייפתח כאן.",
        next: "השאלה הבאה →",
        finishEarlyHint: "ענו נכון על {goal} כדי לסיים",
        back: "חזרה",
        backHome: "חזרה לדף הבית",
        // completion
        doneTitle: "כל הכבוד! 🎉",
        donePraise: { male: "עבודה מדהימה — סיימת את התרגול היומי!", female: "עבודה מדהימה — סיימת את התרגול היומי!", neutral: "עבודה מדהימה — סיימתם את התרגול היומי!" },
        doneBonus: "+{bonus} נקודות בונוס",
        doneStars: "יש לך עכשיו {stars} כוכבים ⭐",
        alreadyDone: "כבר סיימת את התרגול היומי — נתראה מחר! 🌟",
    },
    ru: {
        title: "Ежедневная практика",
        subtitle: "Короткий микс из {goal} задач на вашем уровне — не теряйте серию!",
        progress: "{done}/{goal} верно",
        loading: "Готовим ежедневную практику…",
        loadError: "Не удалось загрузить ежедневную практику.",
        empty: "Сначала начните тренировать тему — затем здесь откроется ежедневная практика.",
        next: "Следующий вопрос →",
        finishEarlyHint: "Ответьте верно на {goal}, чтобы завершить",
        back: "Назад",
        backHome: "На главную",
        // completion
        doneTitle: "Готово! 🎉",
        donePraise: { male: "Отличная работа — вы справились с ежедневной практикой!", female: "Отличная работа — вы справились с ежедневной практикой!", neutral: "Отличная работа — вы справились с ежедневной практикой!" },
        doneBonus: "+{bonus} бонусных очков",
        doneStars: "Теперь у вас {stars} звёзд ⭐",
        alreadyDone: "Вы уже завершили сегодняшнюю практику — до завтра! 🌟",
    },
};

export const getDailyStrings = (language) => getStrings(DAILY_STRINGS, language);
