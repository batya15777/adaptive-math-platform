import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
    withCredentials: true,
});

// Auth routes that legitimately return 401 — do not redirect for these.
// /auth/survey-complete is included so a session-expiry mid-survey does not
// simultaneously fire the global redirect AND the component's catch handler.
const AUTH_PATHS = ["/auth/login", "/auth/register", "/auth/me", "/auth/forgot-password", "/auth/reset-password", "/auth/survey-complete"];

let onUnauthorized = null;
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

api.interceptors.response.use(
    (res) => res,
    (err) => {
        if (err.response?.status === 401 && onUnauthorized) {
            const url = err.config?.url || "";
            const isAuthRoute = AUTH_PATHS.some((p) => url.includes(p));
            if (!isAuthRoute) onUnauthorized();
        }
        return Promise.reject(err);
    }
);

export default api;