import { getStrings } from "../../i18n/languages.js";

// Student Home dashboard strings. Gender-dependent keys are objects { male, female, neutral }
// (the component picks via a safe `g()` helper). Everything else is a plain string.
export const HOME_STRINGS = {
    en: {
        student: "Student",
        wave: "{name} 👋",
        journeyTitle: { male: "Welcome to your learning journey", female: "Welcome to your learning journey", neutral: "Welcome to your learning journey" },
        heroMotivation: { male: "Every question brings you closer to the next star ⭐\nKeep building confidence, understanding and success!", female: "Every question brings you closer to the next star ⭐\nKeep building confidence, understanding and success!", neutral: "Every question brings you closer to the next star ⭐\nKeep building confidence, understanding and success!" },
        journeyBtn: { male: "Continue your journey ✨", female: "Continue your journey ✨", neutral: "Continue your journey ✨" },
        loading: "Loading your dashboard…",
        loadError: "Couldn't load your dashboard.",

        continueCardTitle: { male: "Continue where you left off ✨", female: "Continue where you left off ✨", neutral: "Continue where you left off ✨" },
        continueLevel: "Level {level}",
        continuePicktopic: "Pick a topic and start practicing.",
        continueBtn: { male: "Continue practice", female: "Continue practice", neutral: "Continue practice" },

        starsTitle: "My stars",
        streakTitle: "Daily streak",
        streakDays: "{count} days in a row",
        rankTitle: "My rank",
        rankPlace: "Rank {rank}",
        rankUnranked: "Top 10+",
        rankPraise: { male: "Great job! Keep it up!", female: "Great job! Keep it up!", neutral: "Great job! Keep it up!" },

        dailyTitle: "Daily practice",
        dailyMotivation: { male: "Spend a few minutes today and do even better tomorrow!", female: "Spend a few minutes today and do even better tomorrow!", neutral: "Spend a few minutes today and do even better tomorrow!" },
        dailyGoal: "{done}/{goal} questions",
        dailyReward: "🌟 Solve {goal} and earn {bonus} stars!",
        dailyDoneBtn: "✅ Done today",
        startBtn: { male: "Start practice", female: "Start practice", neutral: "Start practice" },

        recommendedTitle: "Recommended for you",
        recommendedExplain: { male: "We noticed a topic you find tricky. Focused practice will help you get stronger!", female: "We noticed a topic you find tricky. Focused practice will help you get stronger!", neutral: "We noticed a topic you find tricky. Focused practice will help you get stronger!" },
        recommendedFallback: "Keep practicing to strengthen your skills.",
        subBoxLabel: "Focused practice",
        practiceNowBtn: { male: "Practice now", female: "Practice now", neutral: "Practice now" },

        pathTitle: "My path",
        pathProgressLabel: "Progress by topic",
        pathLevel: "Lvl {level}",
        pathEmpty: "Start practicing to see your progress here.",
        allTopics: "All topics →",

        teaserTitle: "Want to climb the ranking?",
        teaserSub: { male: "Just a bit more practice and you're there!", female: "Just a bit more practice and you're there!", neutral: "Just a bit more practice and you're there!" },
        viewLeaderboard: "View leaderboard →",

        // Admin messages (moved here from Profile settings)
        notificationsHistory: "Messages from your teacher",
        notificationsEmpty: "No messages in the last 7 days",
        notificationsLoading: "Loading messages…",
    },
    he: {
        student: "תלמיד/ה",
        wave: "{name} 👋",
        journeyTitle: { male: "ברוך הבא למסע הלמידה שלך", female: "ברוכה הבאה למסע הלמידה שלך", neutral: "ברוכים הבאים למסע הלמידה שלך" },
        heroMotivation: { male: "כל שאלה מקרבת אותך לכוכב הבא ⭐\nהמשך לפתח ביטחון, להבין ולהצליח!", female: "כל שאלה מקרבת אותך לכוכב הבא ⭐\nהמשיכי לפתח ביטחון, להבין ולהצליח!", neutral: "כל שאלה מקרבת אתכם לכוכב הבא ⭐\nהמשיכו לפתח ביטחון, להבין ולהצליח!" },
        journeyBtn: { male: "המשך במסע שלך ✨", female: "המשיכי במסע שלך ✨", neutral: "המשיכו במסע שלכם ✨" },
        loading: "טוען את הדשבורד שלך…",
        loadError: "לא ניתן לטעון את הדשבורד.",

        continueCardTitle: { male: "המשך מאיפה שעצרת ✨", female: "המשיכי מאיפה שעצרת ✨", neutral: "המשיכו מאיפה שעצרתם ✨" },
        continueLevel: "רמה {level}",
        continuePicktopic: "בחרו נושא והתחילו לתרגל.",
        continueBtn: { male: "המשך תרגול", female: "המשיכי תרגול", neutral: "המשיכו תרגול" },

        starsTitle: "הכוכבים שלי",
        streakTitle: "רצף יומי",
        streakDays: "{count} ימים ברצף",
        rankTitle: "הדירוג שלי",
        rankPlace: "מקום {rank}",
        rankUnranked: "מחוץ ל-10",
        rankPraise: { male: "כל הכבוד! המשך כך!", female: "כל הכבוד! המשיכי כך!", neutral: "כל הכבוד! המשיכו כך!" },

        dailyTitle: "תרגול יומי",
        dailyMotivation: { male: "הקדש כמה דקות היום לעצמך ותצליח יותר מחר!", female: "הקדישי כמה דקות היום לעצמך ותצליחי יותר מחר!", neutral: "הקדישו כמה דקות היום לעצמכם ותצליחו יותר מחר!" },
        dailyGoal: "{done}/{goal} שאלות",
        dailyReward: "🌟 פתרו {goal} וקבלו {bonus} כוכבים!",
        dailyDoneBtn: "✅ הושלם היום",
        startBtn: { male: "התחל תרגול", female: "התחילי תרגול", neutral: "התחילו תרגול" },

        recommendedTitle: "מומלץ עבורך",
        recommendedExplain: { male: "שמנו לב שאתה מתקשה בנושא חדש. תרגול ממוקד יעזור לך להתחזק!", female: "שמנו לב שאת מתקשה בנושא חדש. תרגול ממוקד יעזור לך להתחזק!", neutral: "שמנו לב שאתם מתקשים בנושא חדש. תרגול ממוקד יעזור לכם להתחזק!" },
        recommendedFallback: "המשיכו לתרגל כדי לחזק את היכולות שלכם.",
        subBoxLabel: "הזמנת תרגול ממוקדת",
        practiceNowBtn: { male: "להתאמן עכשיו", female: "להתאמן עכשיו", neutral: "להתאמן עכשיו" },

        pathTitle: "המסלול שלי",
        pathProgressLabel: "התקדמות לפי נושאים",
        pathLevel: "רמה {level}",
        pathEmpty: "התחילו לתרגל כדי לראות כאן את ההתקדמות שלכם.",
        allTopics: "לכל הנושאים →",

        teaserTitle: "רוצה לעלות בדירוג?",
        teaserSub: { male: "רק עוד קצת תרגול ואתה שם!", female: "רק עוד קצת תרגול ואת שם!", neutral: "רק עוד קצת תרגול ואתם שם!" },
        viewLeaderboard: "לצפייה בטבלת המובילים →",

        // הודעות מנהל (הועברו לכאן מהגדרות הפרופיל)
        notificationsHistory: "הודעות מהמורה",
        notificationsEmpty: "אין הודעות מ-7 הימים האחרונים",
        notificationsLoading: "טוען הודעות…",
    },
    ru: {
        student: "Ученик",
        wave: "{name} 👋",
        journeyTitle: { male: "Добро пожаловать в ваш путь обучения", female: "Добро пожаловать в ваш путь обучения", neutral: "Добро пожаловать в ваш путь обучения" },
        heroMotivation: { male: "Каждый вопрос приближает вас к следующей звезде ⭐\nРазвивайте уверенность, понимание и успех!", female: "Каждый вопрос приближает вас к следующей звезде ⭐\nРазвивайте уверенность, понимание и успех!", neutral: "Каждый вопрос приближает вас к следующей звезде ⭐\nРазвивайте уверенность, понимание и успех!" },
        journeyBtn: { male: "Продолжить путь ✨", female: "Продолжить путь ✨", neutral: "Продолжить путь ✨" },
        loading: "Загрузка панели…",
        loadError: "Не удалось загрузить панель.",

        continueCardTitle: { male: "Продолжить с того места, где остановились ✨", female: "Продолжить с того места, где остановились ✨", neutral: "Продолжить с того места, где остановились ✨" },
        continueLevel: "Уровень {level}",
        continuePicktopic: "Выберите тему и начните тренироваться.",
        continueBtn: { male: "Продолжить", female: "Продолжить", neutral: "Продолжить" },

        starsTitle: "Мои звёзды",
        streakTitle: "Дни подряд",
        streakDays: "{count} дн. подряд",
        rankTitle: "Моё место",
        rankPlace: "Место {rank}",
        rankUnranked: "Топ 10+",
        rankPraise: { male: "Отлично! Так держать!", female: "Отлично! Так держать!", neutral: "Отлично! Так держать!" },

        dailyTitle: "Ежедневная практика",
        dailyMotivation: { male: "Уделите несколько минут сегодня — и завтра получится ещё лучше!", female: "Уделите несколько минут сегодня — и завтра получится ещё лучше!", neutral: "Уделите несколько минут сегодня — и завтра получится ещё лучше!" },
        dailyGoal: "{done}/{goal} задач",
        dailyReward: "🌟 Решите {goal} и получите {bonus} звёзд!",
        dailyDoneBtn: "✅ Сделано сегодня",
        startBtn: { male: "Начать", female: "Начать", neutral: "Начать" },

        recommendedTitle: "Рекомендуем вам",
        recommendedExplain: { male: "Мы заметили тему, которая даётся непросто. Сфокусированная практика поможет!", female: "Мы заметили тему, которая даётся непросто. Сфокусированная практика поможет!", neutral: "Мы заметили тему, которая даётся непросто. Сфокусированная практика поможет!" },
        recommendedFallback: "Продолжайте тренироваться, чтобы укрепить навыки.",
        subBoxLabel: "Сфокусированная практика",
        practiceNowBtn: { male: "Тренироваться", female: "Тренироваться", neutral: "Тренироваться" },

        pathTitle: "Мой путь",
        pathProgressLabel: "Прогресс по темам",
        pathLevel: "Ур. {level}",
        pathEmpty: "Начните практиковаться, чтобы увидеть прогресс здесь.",
        allTopics: "Все темы →",

        teaserTitle: "Хотите подняться в рейтинге?",
        teaserSub: { male: "Ещё немного практики — и вы там!", female: "Ещё немного практики — и вы там!", neutral: "Ещё немного практики — и вы там!" },
        viewLeaderboard: "Открыть таблицу лидеров →",

        // Сообщения от администратора (перенесены сюда из настроек профиля)
        notificationsHistory: "Сообщения от учителя",
        notificationsEmpty: "Нет сообщений за последние 7 дней",
        notificationsLoading: "Загрузка сообщений…",
    },
};

export const getHomeStrings = (language) => getStrings(HOME_STRINGS, language);
