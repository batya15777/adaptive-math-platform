import { getStrings } from "../i18n/languages.js";

// Global navigation strings (top Navbar + student DashboardLayout sidebar),
// keyed by ISO language code (English is the fallback base).
export const NAV_STRINGS = {
    en: {
        greeting: "Hello",
        logout: "Log out",
        // student shell (DashboardLayout)
        brand: "Mathematics Game",
        home: "Home",
        myDashboard: "My Dashboard",
        mathTraining: "Math Training",
        settings: "Settings",
    },
    he: {
        greeting: "שלום",
        logout: "התנתק",
        // student shell (DashboardLayout)
        brand: "משחק המתמטיקה",
        home: "בית",
        myDashboard: "הלוח שלי",
        mathTraining: "אימון במתמטיקה",
        settings: "הגדרות",
    },
    ru: {
        greeting: "Привет",
        logout: "Выйти",
        // student shell (DashboardLayout)
        brand: "Математическая игра",
        home: "Главная",
        myDashboard: "Моя панель",
        mathTraining: "Тренировка по математике",
        settings: "Настройки",
    },
};

export const getNavStrings = (language) => getStrings(NAV_STRINGS, language);
