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
        // primary nav (short labels for the top-bar pill)
        navHome: "Home",
        navStats: "Statistics",
        navTraining: "Practice",
        navLeaderboard: "Leaderboard",
        navProfile: "Profile",
        navGames: "Games",
        navAria: "Main navigation",
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
        // primary nav (short labels for the top-bar pill)
        navHome: "בית",
        navStats: "סטטיסטיקה",
        navTraining: "תרגול",
        navLeaderboard: "מובילים",
        navProfile: "פרופיל",
        navGames: "משחקים",
        navAria: "ניווט ראשי",
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
        // primary nav (short labels for the top-bar pill)
        navHome: "Главная",
        navStats: "Статистика",
        navTraining: "Практика",
        navLeaderboard: "Лидеры",
        navProfile: "Профиль",
        navGames: "Игры",
        navAria: "Основная навигация",
    },
};

export const getNavStrings = (language) => getStrings(NAV_STRINGS, language);
