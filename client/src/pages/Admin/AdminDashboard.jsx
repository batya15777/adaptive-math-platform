import { useContext } from "react";
import { AuthContext } from "../../context/AuthContextSetup.js";

// Phase 0: a minimal page just to verify admin entry works.
// Admin management modules (Users / Content / Analytics) come in later phases.
export const AdminDashboard = () => {
    const { user } = useContext(AuthContext);

    return (
        <div style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <h1>Admin Dashboard</h1>
            <p>ברוך הבא, {user?.username} (role: {user?.role}).</p>
            <p style={{ color: "#888" }}>
                שלב 0 — אימות כניסה. מודולי הניהול ייבנו בשלבים הבאים.
            </p>
        </div>
    );
};
