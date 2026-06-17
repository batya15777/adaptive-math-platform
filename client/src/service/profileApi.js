import api from './api.js';

export const getProfile  = ()     => api.get('/profile');
export const updateProfile = (data) => api.put('/profile', data);
