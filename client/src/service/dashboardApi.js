import api from './api.js';

// Student Learning Dashboard endpoints (scoped to the logged-in student).
export const getDashboardData = () => api.get('/api/student/dashboard-data');
export const getMyCluster     = () => api.get('/api/student/my-cluster');
