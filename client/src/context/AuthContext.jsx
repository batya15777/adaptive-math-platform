import { useState, useEffect } from "react";
import { AuthContext } from "./AuthContextSetup.js";
import { getMe } from "../service/authApi.js";
import { setUnauthorizedHandler } from "../service/api.js";

export const AuthProvider = ({ children }) => {
    const [user, setUser]               = useState(null);
    const [authLoading, setAuthLoading] = useState(true);

    // Global 401 handler — any expired-session API response clears state and
    // redirects to login, preventing a blank white-screen crash.
    useEffect(() => {
        setUnauthorizedHandler(() => {
            setUser(null);
            window.location.replace("/login");
        });
    }, []);

    // On every page load, ask the server if the session cookie is still valid.
    // This is the only way to restore the user after a browser refresh.
    useEffect(() => {
        getMe()
            .then(res => setUser(res.data))
            .catch(() => setUser(null))
            .finally(() => setAuthLoading(false));
    }, []);

    const loginUser       = (userData) => setUser(userData);
    const logoutUser      = () => setUser(null);
    // Patches the in-memory user after the survey is persisted server-side,
    // so the gate in App.jsx lifts immediately without a full page reload.
    const completeSurvey  = () => setUser(u => u ? { ...u, hasCompletedSurvey: true } : u);

    return (
        <AuthContext.Provider value={{ user, authLoading, loginUser, logoutUser, completeSurvey }}>
            {children}
        </AuthContext.Provider>
    );
};
