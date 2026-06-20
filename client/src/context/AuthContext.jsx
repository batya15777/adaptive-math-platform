import { useState, useEffect } from "react";
import { AuthContext } from "./AuthContextSetup.js";
import { getMe } from "../service/authApi.js";

export const AuthProvider = ({ children }) => {
    const [user, setUser]               = useState(null);
    const [authLoading, setAuthLoading] = useState(true);

    // On every page load, ask the server if the session cookie is still valid.
    // This is the only way to restore the user after a browser refresh.
    useEffect(() => {
        getMe()
            .then(res => setUser(res.data))
            .catch(() => setUser(null))
            .finally(() => setAuthLoading(false));
    }, []);

    const loginUser  = (userData) => setUser(userData);
    const logoutUser = () => setUser(null);

    return (
        <AuthContext.Provider value={{ user, authLoading, loginUser, logoutUser }}>
            {children}
        </AuthContext.Provider>
    );
};
