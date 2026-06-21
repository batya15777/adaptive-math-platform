import api from "./api";

// All admin-only calls live here. Uses the shared axios instance (withCredentials).
export const getUsers = (page = 0, size = 20) =>
    api.get("/admin/users", { params: { page, size } });

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
