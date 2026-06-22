import api from "./api";

// All admin-only calls live here. Uses the shared axios instance (withCredentials).
export const getUsers = (page = 0, size = 20, search = "", role = "", status = "") => {
    const params = { page, size };
    if (search && search.trim()) params.search = search.trim();
    if (role) params.role = role;          // "STUDENT" | "ADMIN" | "" (All)
    if (status) params.status = status;    // "ACTIVE" | "BLOCKED" | "DELETED" | "ALL" | "" (default: active+blocked)
    return api.get("/admin/users", { params });
};

// Change a user's role (STUDENT/ADMIN). Guarded server-side (validateAdminOnly).
export const updateUserRole = (id, role) => api.patch(`/admin/users/${id}/role`, { role });

// Block/unblock a user via account status (ACTIVE/BLOCKED). Guarded server-side.
export const setUserStatus = (id, accountStatus) => api.patch(`/admin/users/${id}/status`, { accountStatus });

// Edit a user's basic details (fullName/email/age/gender). Guarded server-side.
export const updateUser = (id, data) => api.patch(`/admin/users/${id}`, data);

// Read-only platform analytics for the admin dashboard.
export const getAnalytics = () => api.get("/admin/analytics");

// Send a one-shot broadcast message to all currently connected students.
export const sendBroadcast = (message) => api.post("/admin/broadcast", { message });

// Read-only ML cluster groups (existing clustering results).
export const getMLGroups = () => api.get("/admin/ml-groups");

// Admin-only: trigger a clustering run (POST /ml/clustering/run, guarded server-side).
export const runClustering = () => api.post("/ml/clustering/run");

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
