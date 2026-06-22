import api from "./api";

export const register = (data)=>{
    return api.post("/auth/register", data)
}
export const verifyCode = (data) => {
    return api.post("/auth/verify",  data);
}

export const login = (data) => {
    return api.post("/auth/login", data);
}

export const logout = () => {
    return api.post("/auth/logout");
}

export const getMe = () => api.get("/auth/me");

// Password reset flow.
export const forgotPassword = (email) => api.post("/auth/forgot-password", { email });
export const resetPassword = (data) => api.post("/auth/reset-password", data);
