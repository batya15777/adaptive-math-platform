// Local, isolated fallbacks for data the backend doesn't expose yet (daily streak +
// daily practice goal). Kept here so no mock data leaks into the components. If/when
// the API exposes these, swap these helpers for the real values.

const todayKey = () => new Date().toISOString().slice(0, 10);
const yesterdayKey = () => new Date(Date.now() - 86400000).toISOString().slice(0, 10);

const read = (key) => {
    try { return JSON.parse(localStorage.getItem(key) || "null"); } catch { return null; }
};
const write = (key, val) => {
    try { localStorage.setItem(key, JSON.stringify(val)); } catch { /* private mode / quota */ }
};

// Daily streak = number of consecutive days the student opened Home. Real engagement
// signal (not random), persisted locally per user.
export function bumpDailyStreak(userId) {
    const key = `mg_streak_${userId}`;
    const data = read(key);
    const t = todayKey();
    let next;
    if (!data) next = { date: t, count: 1 };
    else if (data.date === t) next = data;                       // already counted today
    else next = { date: t, count: data.date === yesterdayKey() ? (data.count || 0) + 1 : 1 };
    write(key, next);
    return next.count;
}

// Today's practice progress toward a daily goal. `done` resets each day. (Currently a
// local counter; can be incremented from the practice screen later.)
export function getDailyProgress(userId, goal = 5) {
    const data = read(`mg_daily_${userId}`);
    const done = data && data.date === todayKey() ? (data.count || 0) : 0;
    return { done: Math.min(done, goal), goal };
}
