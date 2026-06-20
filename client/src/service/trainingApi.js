import api from './api.js';

export const getTopics      = ()          => api.get('/training/topics');
export const getSubjects    = (topicId)   => api.get(`/training/topics/${topicId}/subjects`);
export const getSubSubjects = (subjectId) => api.get(`/training/subjects/${subjectId}/sub-subjects`);
