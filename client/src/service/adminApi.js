import api from "./api";

// All admin-only calls live here. Uses the shared axios instance (withCredentials).
export const getUsers = (page = 0, size = 20) =>
    api.get("/admin/users", { params: { page, size } });

// Read-only platform analytics for the admin dashboard.
export const getAnalytics = () => api.get("/admin/analytics");

// Read-only ML cluster groups (existing clustering results).
export const getMLGroups = () => api.get("/admin/ml-groups");

// ── Content management: topics ──────────────────────────────
export const getContentTopics = () => api.get("/admin/content/topics");
export const createTopic = (name) => api.post("/admin/content/topics", { name });
export const updateTopic = (id, name) => api.put(`/admin/content/topics/${id}`, { name });
export const setTopicActive = (id, active) =>
    api.put(`/admin/content/topics/${id}/active`, null, { params: { active } });

// ── Content management: subjects (under a topic) ────────────
export const getTopicSubjects = (topicId) => api.get(`/admin/content/topics/${topicId}/subjects`);
export const createSubject = (topicId, name) => api.post(`/admin/content/topics/${topicId}/subjects`, { name });
export const updateSubject = (id, name) => api.put(`/admin/content/subjects/${id}`, { name });
export const setSubjectActive = (id, active) =>
    api.put(`/admin/content/subjects/${id}/active`, null, { params: { active } });

// ── Content management: sub-subjects (under a subject) ──────
export const getSubjectSubSubjects = (subjectId) => api.get(`/admin/content/subjects/${subjectId}/sub-subjects`);
export const createSubSubject = (subjectId, name) => api.post(`/admin/content/subjects/${subjectId}/sub-subjects`, { name });
export const updateSubSubject = (id, name) => api.put(`/admin/content/sub-subjects/${id}`, { name });
export const setSubSubjectActive = (id, active) =>
    api.put(`/admin/content/sub-subjects/${id}/active`, null, { params: { active } });
