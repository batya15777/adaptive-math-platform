import { useState } from "react";
import { AuthContext } from "./AuthContextSetup.js";

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);

    const loginUser = (userData) => {
        setUser(userData);
    };

    const logoutUser = () => {
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, loginUser, logoutUser }}>
            {children}
        </AuthContext.Provider>
    );
}
