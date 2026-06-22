import { getStrings } from "../../i18n/languages.js";

// All Student Statistics strings (container + every widget), keyed by ISO code.
// English is the fallback base (see getStrings). Use format() for {name}/{percent}.
//
// Gender-dependent keys are { male, female, neutral } objects (picked via a safe g()
// helper in the component). Topic + badge name translations live here too (the backend
// sends them in English); see dashboardI18n.js for the lookup helpers.
//
// NOTE: recommendation titles/messages and the badge *descriptions* are produced by the
// backend and rendered as-is; the badge display *names* are localized below by key.
export const DASHBOARD_STRINGS = {
    en: {
        // container
        loading: "Loading your statistics…",
        loadError: "Could not load your statistics. Please try again.",
        statsTitle: "My statistics",
        subtitle: {
            male: "Here you can see your progress, your strengths and the topics worth practising.",
            female: "Here you can see your progress, your strengths and the topics worth practising.",
            neutral: "Here you can see your progress, your strengths and the topics worth practising.",
        },
        // overview cards
        overviewTitle: "Overall performance 📊",
        gotItRight: "success rate",
        questionsAnswered: "Questions answered",
        correctAnswers: "Correct answers",
        levelShort: "Level",
        topLevelReached: "Highest level reached",
        soFar: "So far",
        // topic progress
        topicProgressTitle: "Progress by topic 📚",
        noPracticeYet: "No practice yet — head to Math Training to get started.",
        successLabel: "Success",
        // skill radar
        skillMapTitle: "My skill map 🕸️",
        skillMapSubtitle: "The map shows your strengths by topic.",
        skillYou: "Your level",
        skillGroupAvg: "Group average",
        // learning group
        clusterTitle: "My learning group 👥",
        clusterPending: "We're still getting to know how you learn — keep practising and we'll place you in a learning group.",
        youreInTheGroup: "You're in the group",
        clusterMatch: "{percent}% match with the group",
        clusterExplain: "The system uses this to pick questions that match your current progress level.",
        practicingTogether: "Shared focus areas:",
        // badges
        badgesTitle: "My achievements 🏆",
        earned: "Earned",
        // topic name translations (backend sends English)
        topicNames: {
            add: "Addition", sub: "Subtraction", mult: "Multiplication",
            div: "Division", fractions: "Fractions", decimals: "Decimals",
        },
        // badge display-name translations, keyed by a normalized identity
        badgeNames: {
            firstSteps:   { male: "First steps", female: "First steps", neutral: "First steps" },
            starCollector:{ male: "Star collector", female: "Star collector", neutral: "Star collector" },
            persistent:   { male: "Persistent", female: "Persistent", neutral: "Persistent" },
            explorer:     { male: "Explorer", female: "Explorer", neutral: "Explorer" },
            sharpShooter: { male: "Sharp shooter", female: "Sharp shooter", neutral: "Sharp shooter" },
            risingStar:   { male: "Rising star", female: "Rising star", neutral: "Rising star" },
        },
        // prettified cluster labels (kid-friendly group names)
        groupDefault: "My group 🌟",
        groupMathStar: "Math Stars ⭐",
        groupJustStarting: "Just Starting 🚀",
        groupRisingStar: "Rising Stars 🌱",
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
        loading: "טוען את הסטטיסטיקה שלך…",
        loadError: "לא ניתן לטעון את הסטטיסטיקה שלך. אנא נסו שוב.",
        statsTitle: "הסטטיסטיקה שלי",
        subtitle: {
            male: "כאן תוכל לראות את ההתקדמות שלך, החוזקות שלך והנושאים שכדאי לחזק.",
            female: "כאן תוכלי לראות את ההתקדמות שלך, החוזקות שלך והנושאים שכדאי לחזק.",
            neutral: "כאן תוכלו לראות את ההתקדמות שלכם, החוזקות שלכם והנושאים שכדאי לחזק.",
        },
        // overview cards
        overviewTitle: "ביצועים כלליים 📊",
        gotItRight: "אחוז הצלחה כללי",
        questionsAnswered: "שאלות שנענו",
        correctAnswers: "תשובות נכונות",
        levelShort: "רמה",
        topLevelReached: "הרמה הגבוהה ביותר",
        soFar: "סה\"כ עד היום",
        // topic progress
        topicProgressTitle: "התקדמות לפי נושא 📚",
        noPracticeYet: "עדיין אין תרגול — עברו לאימון במתמטיקה כדי להתחיל.",
        successLabel: "הצלחה",
        // skill radar
        skillMapTitle: "מפת המיומנויות שלי 🕸️",
        skillMapSubtitle: "המפה מציגה את החוזקות שלך לפי נושאים.",
        skillYou: "היכולת שלך",
        skillGroupAvg: "ממוצע בקבוצה",
        // learning group
        clusterTitle: "קבוצת הלמידה שלי 👥",
        clusterPending: "אנחנו עדיין לומדים איך אתם לומדים — המשיכו לתרגל ונשבץ אתכם בקבוצת למידה.",
        youreInTheGroup: "אתם בקבוצה",
        clusterMatch: "{percent}% התאמה לקבוצה",
        clusterExplain: "המערכת משתמשת בזה כדי לבחור עבורך שאלות שמתאימות לרמת ההתקדמות שלך.",
        practicingTogether: "תחומי מיקוד משותפים:",
        // badges
        badgesTitle: "ההישגים שלי 🏆",
        earned: "הושג",
        // topic name translations
        topicNames: {
            add: "חיבור", sub: "חיסור", mult: "כפל",
            div: "חילוק", fractions: "שברים", decimals: "מספרים עשרוניים",
        },
        // badge display-name translations (gender-aware)
        badgeNames: {
            firstSteps:   { male: "צעדים ראשונים", female: "צעדים ראשונים", neutral: "צעדים ראשונים" },
            starCollector:{ male: "אספן כוכבים", female: "אספנית כוכבים", neutral: "אספני כוכבים" },
            persistent:   { male: "מתמיד", female: "מתמידה", neutral: "מתמידים" },
            explorer:     { male: "חוקר", female: "חוקרת", neutral: "חוקרים" },
            sharpShooter: { male: "קלע חד", female: "קלעית חדה", neutral: "קלעים חדים" },
            risingStar:   { male: "כוכב עולה", female: "כוכבת עולה", neutral: "כוכבים עולים" },
        },
        // prettified cluster labels (kid-friendly group names)
        groupDefault: "הקבוצה שלי 🌟",
        groupMathStar: "כוכבי המתמטיקה ⭐",
        groupJustStarting: "רק מתחילים 🚀",
        groupRisingStar: "כוכבים עולים 🌱",
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
        loading: "Загрузка вашей статистики…",
        loadError: "Не удалось загрузить вашу статистику. Попробуйте ещё раз.",
        statsTitle: "Моя статистика",
        subtitle: {
            male: "Здесь вы видите свой прогресс, сильные стороны и темы, которые стоит подтянуть.",
            female: "Здесь вы видите свой прогресс, сильные стороны и темы, которые стоит подтянуть.",
            neutral: "Здесь вы видите свой прогресс, сильные стороны и темы, которые стоит подтянуть.",
        },
        // overview cards
        overviewTitle: "Общие показатели 📊",
        gotItRight: "процент успеха",
        questionsAnswered: "Отвечено вопросов",
        correctAnswers: "Верных ответов",
        levelShort: "Уровень",
        topLevelReached: "Высший уровень",
        soFar: "Всего на данный момент",
        // topic progress
        topicProgressTitle: "Прогресс по темам 📚",
        noPracticeYet: "Пока нет практики — перейдите в «Тренировку по математике», чтобы начать.",
        successLabel: "Успех",
        // skill radar
        skillMapTitle: "Карта моих навыков 🕸️",
        skillMapSubtitle: "Карта показывает ваши сильные стороны по темам.",
        skillYou: "Ваш уровень",
        skillGroupAvg: "Среднее по группе",
        // learning group
        clusterTitle: "Моя учебная группа 👥",
        clusterPending: "Мы ещё узнаём, как вы учитесь — продолжайте тренироваться, и мы определим вас в группу.",
        youreInTheGroup: "Вы в группе",
        clusterMatch: "{percent}% соответствие группе",
        clusterExplain: "Система использует это, чтобы подбирать вопросы под ваш текущий уровень.",
        practicingTogether: "Общие зоны внимания:",
        // badges
        badgesTitle: "Мои достижения 🏆",
        earned: "Получен",
        // topic name translations
        topicNames: {
            add: "Сложение", sub: "Вычитание", mult: "Умножение",
            div: "Деление", fractions: "Дроби", decimals: "Десятичные",
        },
        // badge display-name translations
        badgeNames: {
            firstSteps:   { male: "Первые шаги", female: "Первые шаги", neutral: "Первые шаги" },
            starCollector:{ male: "Коллекционер звёзд", female: "Коллекционер звёзд", neutral: "Коллекционер звёзд" },
            persistent:   { male: "Упорный", female: "Упорная", neutral: "Упорные" },
            explorer:     { male: "Исследователь", female: "Исследовательница", neutral: "Исследователи" },
            sharpShooter: { male: "Меткий стрелок", female: "Меткий стрелок", neutral: "Меткие стрелки" },
            risingStar:   { male: "Восходящая звезда", female: "Восходящая звезда", neutral: "Восходящие звёзды" },
        },
        // prettified cluster labels (kid-friendly group names)
        groupDefault: "Моя группа 🌟",
        groupMathStar: "Звёзды математики ⭐",
        groupJustStarting: "Только начинаю 🚀",
        groupRisingStar: "Восходящие звёзды 🌱",
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
