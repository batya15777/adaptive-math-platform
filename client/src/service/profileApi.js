import api from './api.js';

export const getProfile  = ()     => api.get('/profile');
export const updateProfile = (data) => api.put('/profile', data);
export const getProfileOptions = () => api.get('/profile/options');
// Select an avatar (the server buys it + deducts stars first if it's priced + unowned).
export const selectAvatar = (avatarId) => api.post('/profile/avatar/select', { avatarId });
