import { getStrings } from "../../i18n/languages.js";

// Strings for the Leaderboard widget, keyed by ISO language code.
// English is the fallback base (see getStrings).
export const LEADERBOARD_STRINGS = {
    en: {
        title: "Top 10 Students",
        loading: "Loading leaderboard...",
        loadError: "Could not load the leaderboard.",
        empty: "No entries yet — be the first to earn stars!",
    },
    he: {
        title: "10 התלמידים המובילים",
        loading: "טוען טבלת מובילים...",
        loadError: "לא ניתן לטעון את טבלת המובילים.",
        empty: "אין עדיין רשומות — היו הראשונים לצבור כוכבים!",
    },
    ru: {
        title: "Топ-10 учеников",
        loading: "Загрузка таблицы лидеров...",
        loadError: "Не удалось загрузить таблицу лидеров.",
        empty: "Записей пока нет — станьте первым, кто заработает звёзды!",
    },
};

export const getLeaderboardStrings = (language) => getStrings(LEADERBOARD_STRINGS, language);
