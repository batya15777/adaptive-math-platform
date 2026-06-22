import { getStrings } from "../../i18n/languages.js";

// Strings for the student Profile / Settings page, keyed by ISO code.
// English is the fallback base (see getStrings). Use format() for {n}.
//
// NOTE: the theme/language option *labels* come from the backend
// (getProfileOptions) and are rendered as-is; only the surrounding chrome is
// localized here.
export const PROFILE_SETTINGS_STRINGS = {
    en: {
        pleaseLogIn: "Please log in to view your profile.",
        title: "Profile Settings",
        loading: "Loading...",
        loadError: "Could not load your profile. Please refresh the page.",
        saveSuccess: "Profile updated successfully!",
        saveFailed: "Could not save. Please try again.",
        notSet: "Not set",
        theme: "Theme",
        language: "Language",
        editProfile: "Edit Profile",
        chooseAvatar: "Choose Avatar",
        fullNameReadonly: "Full Name (read-only)",
        emailReadonly: "Email (read-only)",
        saving: "Saving...",
        saveChanges: "Save Changes",
        cancel: "Cancel",
        avatarAlt: "Your avatar",
        avatarAltN: "Avatar {n}",
    },
    he: {
        pleaseLogIn: "אנא התחברו כדי לצפות בפרופיל שלכם.",
        title: "הגדרות פרופיל",
        loading: "טוען...",
        loadError: "לא ניתן לטעון את הפרופיל שלך. אנא רעננו את הדף.",
        saveSuccess: "הפרופיל עודכן בהצלחה!",
        saveFailed: "לא ניתן לשמור. אנא נסו שוב.",
        notSet: "לא הוגדר",
        theme: "ערכת נושא",
        language: "שפה",
        editProfile: "עריכת פרופיל",
        chooseAvatar: "בחרו דמות",
        fullNameReadonly: "שם מלא (לקריאה בלבד)",
        emailReadonly: "אימייל (לקריאה בלבד)",
        saving: "שומר...",
        saveChanges: "שמירת שינויים",
        cancel: "ביטול",
        avatarAlt: "הדמות שלך",
        avatarAltN: "דמות {n}",
    },
    ru: {
        pleaseLogIn: "Войдите, чтобы просмотреть свой профиль.",
        title: "Настройки профиля",
        loading: "Загрузка...",
        loadError: "Не удалось загрузить профиль. Обновите страницу.",
        saveSuccess: "Профиль успешно обновлён!",
        saveFailed: "Не удалось сохранить. Попробуйте ещё раз.",
        notSet: "Не задано",
        theme: "Тема",
        language: "Язык",
        editProfile: "Редактировать профиль",
        chooseAvatar: "Выберите аватар",
        fullNameReadonly: "Полное имя (только чтение)",
        emailReadonly: "Эл. почта (только чтение)",
        saving: "Сохранение...",
        saveChanges: "Сохранить изменения",
        cancel: "Отмена",
        avatarAlt: "Ваш аватар",
        avatarAltN: "Аватар {n}",
    },
};

export const getProfileSettingsStrings = (language) => getStrings(PROFILE_SETTINGS_STRINGS, language);
