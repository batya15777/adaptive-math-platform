// Pure, framework-free logic for the daily-practice session.
// No React / DOM / storage here so it can be unit-tested with `node --test`.

export const DAILY_GOAL = 5;    // correct answers needed to complete the daily practice
export const DAILY_BONUS = 100; // celebratory completion bonus (shown to the student)

// Order the student's topics for a *mixed* daily session: weakest first (lowest success
// rate gets practised soonest), ties broken by fewer attempts. Invalid entries dropped.
// Returns a stable array of { subSubjectId, name }.
export function orderDailyTopics(topics = []) {
    return (topics || [])
        .filter(t => t && t.subSubjectId != null)
        .slice()
        .sort((a, b) =>
            (a.successRate ?? 0) - (b.successRate ?? 0) ||
            (a.attempts ?? 0) - (b.attempts ?? 0))
        .map(t => ({ subSubjectId: t.subSubjectId, name: t.name }));
}

// The topic for the N-th question (0-based), cycling through the ordered list so the
// questions are spread across topics ("mixed"). Returns null when there are no topics.
export function topicForIndex(orderedTopics, index) {
    if (!orderedTopics || orderedTopics.length === 0) return null;
    return orderedTopics[index % orderedTopics.length];
}

// Find a question the student hasn't seen yet, re-rolling across topics (starting at
// `startIndex`) so the daily practice never repeats a question. `fetch(subSubjectId)`
// returns a question (or null). Returns { topic, question, fresh }: fresh=false means the
// question bank was exhausted and we fell back to the last fetched question so the session
// never gets stuck. Returns null when nothing could be fetched.
export async function pickFreshQuestion({ orderedTopics, startIndex = 0, seen, fetch, maxTries }) {
    const list = orderedTopics || [];
    const tries = maxTries ?? Math.max(list.length * 2, 6);
    let last = null;
    let lastTopic = null;
    for (let k = 0; k < tries; k++) {
        const tp = topicForIndex(list, startIndex + k);
        if (!tp) break;
        const q = await fetch(tp.subSubjectId);
        if (!q) continue;
        last = q; lastTopic = tp;
        if (!seen.has(q.questionId)) return { topic: tp, question: q, fresh: true };
    }
    return last ? { topic: lastTopic, question: last, fresh: false } : null;
}

export function isComplete(correctCount, goal = DAILY_GOAL) {
    return correctCount >= goal;
}

export function progressPct(correctCount, goal = DAILY_GOAL) {
    if (goal <= 0) return 0;
    return Math.min(100, Math.round((correctCount / goal) * 100));
}
