import { getStrings } from "../../i18n/languages.js";

// Strings for the Math Training drill-down (topic → subject → sub-subject),
// keyed by ISO code. English is the fallback base (see getStrings).
// Topic/subject/sub-subject *names* come from the backend and are rendered as-is.
export const MATH_TRAINING_STRINGS = {
    en: {
        title: "Math Training",
        topics: "Topics",
        loading: "Loading...",
        errGeneric: "Something went wrong. Please try again.",
        emptyTopics: "No topics available yet.",
        emptySubjects: "No subjects under this topic yet.",
        emptySubSubjects: "No sub-subjects under this subject yet.",
        play: "Play ▶",
        statLevel: "Level",
        statSolved: "Solved",
        statDifficulty: "Difficulty",
    },
    he: {
        title: "אימון במתמטיקה",
        topics: "נושאים",
        loading: "טוען...",
        errGeneric: "משהו השתבש. אנא נסו שוב.",
        emptyTopics: "אין עדיין נושאים זמינים.",
        emptySubjects: "אין עדיין מקצועות תחת נושא זה.",
        emptySubSubjects: "אין עדיין תתי-נושאים תחת מקצוע זה.",
        play: "שחק ▶",
        statLevel: "רמה",
        statSolved: "נפתרו",
        statDifficulty: "קושי",
    },
    ru: {
        title: "Тренировка по математике",
        topics: "Темы",
        loading: "Загрузка...",
        errGeneric: "Что-то пошло не так. Попробуйте ещё раз.",
        emptyTopics: "Пока нет доступных тем.",
        emptySubjects: "Под этой темой пока нет предметов.",
        emptySubSubjects: "Под этим предметом пока нет подтем.",
        play: "Играть ▶",
        statLevel: "Уровень",
        statSolved: "Решено",
        statDifficulty: "Сложность",
    },
};

export const getMathTrainingStrings = (language) => getStrings(MATH_TRAINING_STRINGS, language);
