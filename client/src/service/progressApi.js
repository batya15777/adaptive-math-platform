import api from './api.js';

export const getStatus         = (subSubjectId)           => api.get(`/progress/status/sub-subject/${subSubjectId}`);
export const getNextQuestion   = (subSubjectId, language)  => api.get('/progress/next-question', { params: { subSubjectId, language } });
export const submitAnswer      = (body)                    => api.post('/progress/submit-answer', body);
export const revealSolution    = (body)                    => api.post('/progress/reveal-solution', body);
export const getBonusQuestion  = (subSubjectId, language)  => api.get('/progress/bonus-question', { params: { subSubjectId, language } });
export const submitBonusAnswer = (body)                    => api.post('/progress/bonus-answer', body);
export const initialAssessment = (body)                    => api.post('/progress/initial-assessment', body);
// Claim the daily-practice bonus (server verifies "5 correct today" + once/day).
export const claimDailyBonus   = ()                        => api.post('/progress/daily-bonus');
