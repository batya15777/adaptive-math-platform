// Per-day, per-mode record of which question ids each practice flow has already used today,
// so Daily Practice and Recommended Practice can exclude each other and never serve the same
// question on the same day. Stored in localStorage, keyed by mode + user + date.
//
// Each flow records the ids it shows, and excludes the OTHER flow's ids when asking the server
// for the next question (the server then drops/regenerates any active question that's excluded).

export const PRACTICE_MODE = { DAILY: 'DAILY', RECOMMENDED: 'RECOMMENDED', REGULAR: 'REGULAR' };

const dayStamp = () => {
    const d = new Date();
    return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
};

const keyFor = (mode, userId) => `practice_used_${mode}_${userId}_${dayStamp()}`;

// The question ids a flow has used today (empty when none / no user).
export const getUsedQuestionIds = (mode, userId) => {
    if (userId == null) return [];
    try {
        const raw = localStorage.getItem(keyFor(mode, userId));
        return raw ? JSON.parse(raw) : [];
    } catch {
        return [];
    }
};

// Remember that `mode` showed `questionId` today (idempotent; ignores nulls / quota errors).
export const recordUsedQuestionId = (mode, userId, questionId) => {
    if (userId == null || questionId == null) return;
    const ids = new Set(getUsedQuestionIds(mode, userId));
    if (ids.has(questionId)) return;
    ids.add(questionId);
    try {
        localStorage.setItem(keyFor(mode, userId), JSON.stringify([...ids]));
    } catch {
        /* localStorage full / unavailable — exclusion is best-effort, never block practice */
    }
};
