import api from "./api";

export const askTutor = (data) => {
    return api.post("/api/tutor-chat", data);
};
