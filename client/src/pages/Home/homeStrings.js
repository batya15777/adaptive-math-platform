import { getStrings } from "../../i18n/languages.js";

// Strings for the student Home page, keyed by ISO language code.
// English is the fallback base (see getStrings). Use format() for {name}.
export const HOME_STRINGS = {
    en: {
        student: "Student",
        welcomeBack: "Welcome back, {name}! 👋",
        keepPracticing: "Keep practicing to climb the leaderboard.",
    },
    he: {
        student: "תלמיד/ה",
        welcomeBack: "ברוך/ה שובך, {name}! 👋",
        keepPracticing: "המשיכו להתאמן כדי לטפס בטבלת המובילים.",
    },
    ru: {
        student: "Ученик",
        welcomeBack: "С возвращением, {name}! 👋",
        keepPracticing: "Продолжайте тренироваться, чтобы подняться в таблице лидеров.",
    },
};

export const getHomeStrings = (language) => getStrings(HOME_STRINGS, language);
