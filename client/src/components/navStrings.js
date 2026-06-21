import { getStrings } from "../i18n/languages.js";

// Global navbar strings, keyed by ISO language code (English is the fallback base).
export const NAV_STRINGS = {
    en: { greeting: "Hello", logout: "Log out" },
    he: { greeting: "שלום", logout: "התנתק" },
    ru: { greeting: "Привет", logout: "Выйти" },
};

export const getNavStrings = (language) => getStrings(NAV_STRINGS, language);
