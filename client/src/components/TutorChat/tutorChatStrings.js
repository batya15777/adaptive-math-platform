// UI strings for the tutor chat, keyed by language code.
//
// To add a new language:
//   1. Add one entry to TUTOR_CHAT_STRINGS below (copy an existing block, translate).
//   2. Language direction/locale primitives are shared — see i18n/languages.js.
// Missing keys automatically fall back to DEFAULT_LANGUAGE, so a partial
// translation still renders without breaking.

import { DEFAULT_LANGUAGE, isRtl, localeFor } from "../../i18n/languages.js";

export const TUTOR_CHAT_STRINGS = {
    en: {
        eyebrow: "Exercise help",
        title: "AI Tutor",
        close: "Close",
        openHint: "Need help? Tap me",
        placeholder: "Ask a question about the exercise...",
        history: "🕘 History",
        back: "← Back",
        senderStudent: "Me",
        senderTutor: "Tutor",
        emptyChat: "Stuck? Ask me a question about the exercise and I'll guide you step by step.",
        thinking: "One moment, I'm thinking how to guide you...",
        send: "Send",
        noQuestion: "To use the chat, a question id must be provided.",
        loadingConversations: "Loading conversations...",
        noConversations: "No saved conversations yet.",
        loading: "Loading...",
        viewingPrefix: "Viewing a past conversation: ",
        emptyConversation: "No messages in this conversation.",
        messages: "messages",
        errNotFound: "No matching question was found for the chat. Add a question to the table or pass an existing question id.",
        errAuth: "Please log in again to use the chat.",
        errBadRequest: "The chat is missing a question id or message text.",
        errGeneric: "I couldn't get a response right now",
        errLoadConversation: "I couldn't load the conversation.",
    },
    he: {
        eyebrow: "עזרה בתרגיל",
        title: "AI Tutor",
        close: "סגירה",
        openHint: "צריך עזרה? לחצו עליי",
        placeholder: "כתבי שאלה על התרגיל...",
        history: "🕘 היסטוריה",
        back: "← חזרה",
        senderStudent: "אני",
        senderTutor: "המדריך",
        emptyChat: "נתקעת? שאלי אותי שאלה על התרגיל ואכוון אותך צעד אחרי צעד.",
        thinking: "רגע, אני חושב איך להדריך אותך...",
        send: "שלח",
        noQuestion: "כדי להפעיל את הצ׳אט צריך להעביר לו מזהה שאלה.",
        loadingConversations: "טוען שיחות...",
        noConversations: "עדיין אין שיחות שמורות.",
        loading: "טוען...",
        viewingPrefix: "צפייה בשיחה קודמת: ",
        emptyConversation: "אין הודעות בשיחה הזו.",
        messages: "הודעות",
        errNotFound: "לא נמצאה שאלה במערכת עבור הצ׳אט. צריך להוסיף שאלה לטבלה או להעביר מזהה שאלה קיים.",
        errAuth: "צריך להתחבר מחדש כדי להשתמש בצ׳אט.",
        errBadRequest: "חסר מזהה שאלה או טקסט שאלה לצ׳אט.",
        errGeneric: "לא הצלחתי לקבל תשובה כרגע",
        errLoadConversation: "לא הצלחתי לטעון את השיחה.",
    },
    ru: {
        eyebrow: "Помощь с заданием",
        title: "AI Tutor",
        close: "Закрыть",
        openHint: "Нужна помощь? Нажмите",
        placeholder: "Задайте вопрос по заданию...",
        history: "🕘 История",
        back: "← Назад",
        senderStudent: "Я",
        senderTutor: "Наставник",
        emptyChat: "Застряли? Задайте мне вопрос по заданию, и я помогу вам шаг за шагом.",
        thinking: "Минутку, я думаю, как вам помочь...",
        send: "Отправить",
        noQuestion: "Чтобы использовать чат, нужно передать идентификатор задания.",
        loadingConversations: "Загрузка бесед...",
        noConversations: "Пока нет сохранённых бесед.",
        loading: "Загрузка...",
        viewingPrefix: "Просмотр прошлой беседы: ",
        emptyConversation: "В этой беседе нет сообщений.",
        messages: "сообщений",
        errNotFound: "Для чата не найдено подходящего задания. Добавьте задание в таблицу или передайте существующий id.",
        errAuth: "Пожалуйста, войдите снова, чтобы использовать чат.",
        errBadRequest: "В чате отсутствует id задания или текст сообщения.",
        errGeneric: "Не удалось получить ответ сейчас",
        errLoadConversation: "Не удалось загрузить беседу.",
    },
};

// Resolve the strings for a language, falling back to the default for any missing
// key so a new or partial translation never leaves blanks.
export const getTutorChatStrings = (language) => ({
    ...TUTOR_CHAT_STRINGS[DEFAULT_LANGUAGE],
    ...(TUTOR_CHAT_STRINGS[language] || {}),
});

// Re-export the shared primitives under the names tutor-chat already imports,
// so behavior is identical while the single source of truth is i18n/languages.js.
export { DEFAULT_LANGUAGE, localeFor };
export const isRtlLanguage = isRtl;
