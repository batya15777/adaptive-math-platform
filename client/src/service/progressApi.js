import api from './api.js';

export const getStatus         = (subSubjectId)           => api.get(`/progress/status/sub-subject/${subSubjectId}`);
// excludeQuestionIds: ids the server must NOT return (keeps Daily and Recommended practice
// disjoint on the same day). Sent comma-separated so Spring binds it to List<Long>.
// daily: set only by the Daily Practice flow — turns on the server's early-grade subject
// filter (no polynomial/algebra for grades 1–3). Omitted for Regular Practice.
export const getNextQuestion   = (subSubjectId, language, excludeQuestionIds, daily = false) => api.get('/progress/next-question', {
    params: {
        subSubjectId, language,
        ...(excludeQuestionIds && excludeQuestionIds.length ? { excludeQuestionIds: excludeQuestionIds.join(',') } : {}),
        ...(daily ? { daily: true } : {}),
    },
});
export const submitAnswer      = (body)                    => api.post('/progress/submit-answer', body);
export const revealSolution    = (body)                    => api.post('/progress/reveal-solution', body);
export const getBonusQuestion  = (subSubjectId, language)  => api.get('/progress/bonus-question', { params: { subSubjectId, language } });
export const submitBonusAnswer = (body)                    => api.post('/progress/bonus-answer', body);
export const initialAssessment = (body)                    => api.post('/progress/initial-assessment', body);
// Claim the daily-practice bonus (server verifies "5 correct today" + once/day).
export const claimDailyBonus   = ()                        => api.post('/progress/daily-bonus');
