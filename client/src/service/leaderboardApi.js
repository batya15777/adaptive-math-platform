import api from './api.js';

export const getTop10 = () => api.get('/leaderboard/top10');
