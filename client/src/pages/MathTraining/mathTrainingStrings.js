import { getStrings } from "../../i18n/languages.js";

// Strings for the Training drill-down (subject → topic → practice/levels), keyed by ISO.
// English is the fallback base (see getStrings). The hierarchy is generic (not Math-only):
// the backend may add more subjects later and the same screens render them.
//
// Backend sends the topic/subject/sub-subject *names* in English — `names` below maps the
// known ones to each language; unknown names fall back to the backend value (see the
// trName() helper in MathTraining.jsx), so future subjects keep working.
export const MATH_TRAINING_STRINGS = {
    en: {
        title: "Training",
        loading: "Loading…",
        errGeneric: "Something went wrong. Please try again.",
        emptyTopics: "No subjects available yet.",
        emptySubjects: "No topics under this subject yet.",
        emptySubSubjects: "No practices under this topic yet.",
        // per-step subtitles
        subSubjects: "Practise at your own pace, anytime, anywhere",
        subPillars: "Choose a category to start training",
        subTopics: "Choose a topic to start from",
        subPractices: "Choose a level and start solving",
        // step indicator
        stepSubjects: "Subjects",
        stepTopics: "Topics",
        stepPractices: "Levels",
        stepOf: "Step {n} of {total}",
        // cards
        enterHint: "Choose a topic and start training",
        start: "Start",
        play: "Play",
        statLevel: "Level",
        statSolved: "Solved",
        statDifficulty: "Difficulty",
        // backend name → display name
        names: {
            math: "Mathematics",
            calculation: "Calculation", polynomial: "Polynomials", verbal: "Word problems",
            add: "Addition", sub: "Subtraction", mult: "Multiplication", div: "Division",
            fractions: "Fractions", decimals: "Decimals",
            roads: "Roads", percentages: "Percentages",
            linearEquations: "Linear equations", quadraticEquations: "Quadratic equations",
        },
    },
    he: {
        title: "אימון במתמטיקה",
        loading: "טוען…",
        errGeneric: "משהו השתבש. אנא נסו שוב.",
        emptyTopics: "אין עדיין מקצועות זמינים.",
        emptySubjects: "אין עדיין נושאים תחת מקצוע זה.",
        emptySubSubjects: "אין עדיין תרגולים תחת נושא זה.",
        // per-step subtitles
        subSubjects: "תרגלו בקצב שלכם, בכל זמן ובכל מקום",
        subPillars: "בחרו קטגוריה להתחיל להתאמן",
        subTopics: "בחרו תת נושא שממנו תרצו להתחיל",
        subPractices: "בחרו רמה והתחילו לפתור",
        // step indicator
        stepSubjects: "נושאים",
        stepTopics: "תתי נושאים",
        stepPractices: "רמות",
        stepOf: "שלב {n} מתוך {total}",
        // cards
        enterHint: "בחרו נושא והתחילו להתאמן",
        start: "התחילו",
        play: "שחק",
        statLevel: "רמה",
        statSolved: "נפתרו",
        statDifficulty: "קושי",
        // backend name → display name
        names: {
            math: "מתמטיקה",
            calculation: "חישובים", polynomial: "פולינומים", verbal: "בעיות מילוליות",
            add: "חיבור", sub: "חיסור", mult: "כפל", div: "חילוק",
            fractions: "שברים", decimals: "מספרים עשרוניים",
            roads: "דרכים", percentages: "אחוזים",
            linearEquations: "משוואות לינאריות", quadraticEquations: "משוואות ריבועיות",
        },
    },
    ru: {
        title: "Тренировка по математике",
        loading: "Загрузка…",
        errGeneric: "Что-то пошло не так. Попробуйте ещё раз.",
        emptyTopics: "Пока нет доступных предметов.",
        emptySubjects: "Под этим предметом пока нет тем.",
        emptySubSubjects: "Под этой темой пока нет тренировок.",
        // per-step subtitles
        subSubjects: "Тренируйтесь в своём темпе, в любое время и в любом месте",
        subPillars: "Выберите категорию для тренировки",
        subTopics: "Выберите тему, с которой хотите начать",
        subPractices: "Выберите уровень и начните решать",
        // step indicator
        stepSubjects: "Предметы",
        stepTopics: "Темы",
        stepPractices: "Уровни",
        stepOf: "Шаг {n} из {total}",
        // cards
        enterHint: "Выберите тему и начните тренировку",
        start: "Начать",
        play: "Играть",
        statLevel: "Уровень",
        statSolved: "Решено",
        statDifficulty: "Сложность",
        // backend name → display name
        names: {
            math: "Математика",
            calculation: "Вычисления", polynomial: "Полиномы", verbal: "Текстовые задачи",
            add: "Сложение", sub: "Вычитание", mult: "Умножение", div: "Деление",
            fractions: "Дроби", decimals: "Десятичные",
            roads: "Дороги", percentages: "Проценты",
            linearEquations: "Линейные уравнения", quadraticEquations: "Квадратные уравнения",
        },
    },
};

export const getMathTrainingStrings = (language) => getStrings(MATH_TRAINING_STRINGS, language);
