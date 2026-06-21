import { getStrings } from "../../i18n/languages.js";

// All Student Dashboard strings (container + every widget), keyed by ISO code.
// English is the fallback base (see getStrings). Use format() for {name}.
//
// NOTE: recommendation titles/messages and badge names/descriptions are produced
// by the backend (StudentDashboardService) and are rendered as-is; localizing
// them requires server changes and is out of scope for this frontend dictionary.
export const DASHBOARD_STRINGS = {
    en: {
        // container
        loading: "Loading your dashboard…",
        loadError: "Could not load your dashboard. Please try again.",
        greeting: "Hi {name} 👋",
        greetingFallbackName: "there",
        subtitle: "Here's your personalised learning dashboard.",
        // overview cards
        overviewTitle: "How am I doing? 📊",
        gotItRight: "got it right",
        stars: "Stars",
        questionsAnswered: "Questions answered",
        correctAnswers: "Correct answers",
        levelShort: "Lv",
        topLevelReached: "Top level reached",
        // cluster card
        clusterTitle: "My learning group 🧩",
        clusterPending: "We're still getting to know how you learn! Keep playing and we'll place you in a learning group. 🧩",
        ourScore: "our score",
        youreInTheGroup: "You're in the group",
        perfectQuestions: "We pick the perfect questions just for you! 🎯",
        practicingTogether: "What we're practicing together:",
        // adaptive alerts
        recommendedTitle: "Recommended for you 🎯",
        practiceNow: "Practice now →",
        noRecommendations: "No recommendations right now.",
        practiceFallback: "Practice",
        // topic progress
        topicProgressTitle: "Progress by topic 📚",
        noPracticeYet: "No practice yet — head to Math Training to get started.",
        successLabel: "Success",
        // skill radar
        skillMapTitle: "Your skill map 🕸️",
        // badges
        badgesTitle: "Badges 🏅",
        earned: "Earned",
        // prettified cluster labels (kid-friendly group names)
        groupDefault: "My group 🌟",
        groupMathStar: "Math Star ⭐",
        groupJustStarting: "Just Starting 🚀",
        groupRisingStar: "Rising Star 🌱",
        // prettified error patterns (focus-area chips)
        errConfusedSubAdd: "Mixing plus and minus",
        errMinorCalc: "Tiny slips",
        errEmptyAnswer: "Skipped questions",
        errInvalidFormat: "Writing the answer",
        opMult: "Times tables",
        opAdd: "Adding",
        opSub: "Subtracting",
        opDiv: "Dividing",
    },
    he: {
        // container
        loading: "טוען את הלוח שלך…",
        loadError: "לא ניתן לטעון את הלוח שלך. אנא נסו שוב.",
        greeting: "היי {name} 👋",
        greetingFallbackName: "שם",
        subtitle: "הנה לוח הלמידה האישי שלך.",
        // overview cards
        overviewTitle: "איך אני מתקדם/ת? 📊",
        gotItRight: "תשובות נכונות",
        stars: "כוכבים",
        questionsAnswered: "שאלות שנענו",
        correctAnswers: "תשובות נכונות",
        levelShort: "רמה",
        topLevelReached: "הרמה הגבוהה ביותר",
        // cluster card
        clusterTitle: "קבוצת הלמידה שלי 🧩",
        clusterPending: "אנחנו עדיין לומדים איך אתם לומדים! המשיכו לשחק ונשבץ אתכם בקבוצת למידה. 🧩",
        ourScore: "הציון שלנו",
        youreInTheGroup: "אתם בקבוצה",
        perfectQuestions: "אנחנו בוחרים בדיוק את השאלות המתאימות לכם! 🎯",
        practicingTogether: "על מה אנחנו מתאמנים יחד:",
        // adaptive alerts
        recommendedTitle: "מומלץ עבורך 🎯",
        practiceNow: "להתאמן עכשיו →",
        noRecommendations: "אין המלצות כרגע.",
        practiceFallback: "תרגול",
        // topic progress
        topicProgressTitle: "התקדמות לפי נושא 📚",
        noPracticeYet: "עדיין אין תרגול — עברו לאימון במתמטיקה כדי להתחיל.",
        successLabel: "הצלחה",
        // skill radar
        skillMapTitle: "מפת המיומנויות שלך 🕸️",
        // badges
        badgesTitle: "תגים 🏅",
        earned: "הושג",
        // prettified cluster labels (kid-friendly group names)
        groupDefault: "הקבוצה שלי 🌟",
        groupMathStar: "כוכב מתמטיקה ⭐",
        groupJustStarting: "רק מתחילים 🚀",
        groupRisingStar: "כוכב עולה 🌱",
        // prettified error patterns (focus-area chips)
        errConfusedSubAdd: "בלבול בין חיבור לחיסור",
        errMinorCalc: "טעויות קטנות",
        errEmptyAnswer: "שאלות שדולגו",
        errInvalidFormat: "כתיבת התשובה",
        opMult: "לוח הכפל",
        opAdd: "חיבור",
        opSub: "חיסור",
        opDiv: "חילוק",
    },
    ru: {
        // container
        loading: "Загрузка вашей панели…",
        loadError: "Не удалось загрузить вашу панель. Попробуйте ещё раз.",
        greeting: "Привет, {name} 👋",
        greetingFallbackName: "друг",
        subtitle: "Вот ваша персональная панель обучения.",
        // overview cards
        overviewTitle: "Как у меня дела? 📊",
        gotItRight: "верных ответов",
        stars: "Звёзды",
        questionsAnswered: "Отвечено вопросов",
        correctAnswers: "Верных ответов",
        levelShort: "Ур.",
        topLevelReached: "Высший уровень",
        // cluster card
        clusterTitle: "Моя учебная группа 🧩",
        clusterPending: "Мы ещё узнаём, как вы учитесь! Продолжайте играть, и мы определим вас в учебную группу. 🧩",
        ourScore: "наш результат",
        youreInTheGroup: "Вы в группе",
        perfectQuestions: "Мы подбираем идеальные вопросы именно для вас! 🎯",
        practicingTogether: "Что мы отрабатываем вместе:",
        // adaptive alerts
        recommendedTitle: "Рекомендовано вам 🎯",
        practiceNow: "Тренироваться →",
        noRecommendations: "Сейчас рекомендаций нет.",
        practiceFallback: "Практика",
        // topic progress
        topicProgressTitle: "Прогресс по темам 📚",
        noPracticeYet: "Пока нет практики — перейдите в «Тренировку по математике», чтобы начать.",
        successLabel: "Успех",
        // skill radar
        skillMapTitle: "Карта ваших навыков 🕸️",
        // badges
        badgesTitle: "Значки 🏅",
        earned: "Получен",
        // prettified cluster labels (kid-friendly group names)
        groupDefault: "Моя группа 🌟",
        groupMathStar: "Звезда математики ⭐",
        groupJustStarting: "Только начинаю 🚀",
        groupRisingStar: "Восходящая звезда 🌱",
        // prettified error patterns (focus-area chips)
        errConfusedSubAdd: "Путаница плюса и минуса",
        errMinorCalc: "Мелкие ошибки",
        errEmptyAnswer: "Пропущенные вопросы",
        errInvalidFormat: "Запись ответа",
        opMult: "Таблица умножения",
        opAdd: "Сложение",
        opSub: "Вычитание",
        opDiv: "Деление",
    },
};

export const getDashboardStrings = (language) => getStrings(DASHBOARD_STRINGS, language);
