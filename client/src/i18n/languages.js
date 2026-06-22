// Shared, app-wide i18n primitives. Generic by design: adding a new language
// means adding its enum name to LANG_CODE and a matching key in each strings
// dictionary — no code changes elsewhere.

// ISO code used as the dictionary key throughout the UI, and the ultimate
// fallback when a language has no dictionary entry.
export const DEFAULT_LANGUAGE = "en";

// Maps the backend UserProfile.language enum name → ISO code used by dictionaries.
export const LANG_CODE = {
    HEBREW: "he",
    ENGLISH: "en",
    RUSSIAN: "ru",
};

// enum names accepted as a profile/guest language value.
export const PROFILE_LANGUAGES = Object.keys(LANG_CODE); // ["HEBREW","ENGLISH","RUSSIAN"]

// Guest (pre-login) language persistence. Defaults to HEBREW to preserve today's
// guest behavior. Validated against PROFILE_LANGUAGES so junk in storage is ignored.
const GUEST_LANGUAGE_KEY = "guestLanguage";

export const readGuestLanguage = () => {
    try {
        const v = localStorage.getItem(GUEST_LANGUAGE_KEY);
        return PROFILE_LANGUAGES.includes(v) ? v : "HEBREW";
    } catch {
        return "HEBREW";
    }
};

export const writeGuestLanguage = (lang) => {
    try {
        localStorage.setItem(GUEST_LANGUAGE_KEY, lang);
    } catch {
        /* ignore storage failures (private mode, quota) */
    }
};

// Guest (pre-login) theme persistence — so a chosen theme survives a refresh
// instead of snapping back to the default. Logged-in users get theme from the server.
const GUEST_THEME_KEY = "guestTheme";
const PROFILE_THEMES = ["LIGHT", "DARK"];

export const readGuestTheme = () => {
    try {
        const v = localStorage.getItem(GUEST_THEME_KEY);
        return PROFILE_THEMES.includes(v) ? v : "LIGHT";
    } catch {
        return "LIGHT";
    }
};

export const writeGuestTheme = (theme) => {
    try {
        localStorage.setItem(GUEST_THEME_KEY, theme);
    } catch {
        /* ignore storage failures (private mode, quota) */
    }
};

// Right-to-left scripts. Future RTL languages just join the set.
export const RTL_LANGUAGES = new Set(["he", "ar", "fa", "ur"]);

// Locale strings for Intl/date formatting, keyed by ISO code.
export const LOCALE_BY_LANGUAGE = {
    he: "he-IL",
    en: "en-US",
    ru: "ru-RU",
};

// enum name ("HEBREW") → ISO code ("he"), falling back to the default language.
export const toLangCode = (profileLanguage) =>
    LANG_CODE[profileLanguage] || DEFAULT_LANGUAGE;

export const isRtl = (code) => RTL_LANGUAGES.has(code);

export const localeFor = (code) =>
    LOCALE_BY_LANGUAGE[code] || LOCALE_BY_LANGUAGE[DEFAULT_LANGUAGE];

// Merges a per-language dictionary with the default-language base so missing
// keys always fall back to readable text instead of rendering blank.
export const getStrings = (dict, code) => ({
    ...dict[DEFAULT_LANGUAGE],
    ...(dict[code] || {}),
});

// Tiny interpolation helper: format("Page {page} of {total}", { page: 1, total: 5 }).
// Unknown placeholders are left intact so missing data is visible, not silent.
export const format = (template, vars = {}) =>
    String(template).replace(/\{(\w+)\}/g, (_, key) =>
        Object.prototype.hasOwnProperty.call(vars, key) ? vars[key] : `{${key}}`);
