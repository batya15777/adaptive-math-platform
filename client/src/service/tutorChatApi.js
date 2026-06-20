import api from "./api";

export const askTutor = (data) => {
    return api.post("/api/tutor-chat", data);
};

// Saved messages for one question — restores the chat on mount and powers the history view.
export const getTutorMessages = (questionId) => {
    return api.get(`/api/tutor-chat/${questionId}/messages`);
};

// List of past conversations (one per question) for the history panel.
export const getTutorConversations = () => {
    return api.get("/api/tutor-chat/conversations");
};
