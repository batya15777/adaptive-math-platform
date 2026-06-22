import { getStrings } from "../i18n/languages.js";

// Global navigation strings (top Navbar + student DashboardLayout sidebar),
// keyed by ISO language code (English is the fallback base).
export const NAV_STRINGS = {
    en: {
        greeting: "Hello",
        logout: "Log out",
        back: "Back",
        // student shell (DashboardLayout)
        brand: "Mathematics Game",
        home: "Home",
        myDashboard: "My Dashboard",
        mathTraining: "Math Training",
        settings: "Settings",
        leaderboard: "Leaderboard",
    },
    he: {
        greeting: "שלום",
        logout: "התנתק",
        back: "חזרה",
        // student shell (DashboardLayout)
        brand: "משחק המתמטיקה",
        home: "בית",
        myDashboard: "הלוח שלי",
        mathTraining: "אימון במתמטיקה",
        settings: "הגדרות",
        leaderboard: "טבלת מובילים",
    },
    ru: {
        greeting: "Привет",
        logout: "Выйти",
        back: "Назад",
        // student shell (DashboardLayout)
        brand: "Математическая игра",
        home: "Главная",
        myDashboard: "Моя панель",
        mathTraining: "Тренировка по математике",
        settings: "Настройки",
        leaderboard: "Таблица лидеров",
    },
};

export const getNavStrings = (language) => getStrings(NAV_STRINGS, language);
