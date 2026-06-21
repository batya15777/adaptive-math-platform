import { useContext } from "react";
import { Link } from "react-router-dom";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";

// Phase 0: a minimal page just to verify admin entry works.
// Admin management modules (Users / Content / Analytics) come in later phases.
export const AdminDashboard = () => {
    const { user } = useContext(AuthContext);
    const { language } = useLanguage();
    const t = getAdminStrings(language);

    return (
        <div style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <h1>{t.dashboardTitle}</h1>
            <p>{t.welcome}, {user?.username} ({t.roleLabel}: {user?.role}).</p>
            <p style={{ color: "#888" }}>{t.dashboardNote}</p>
            <p><Link to="/admin/users">{t.manageUsers}</Link></p>
            <p><Link to="/admin/topics">{t.manageTopics}</Link></p>
        </div>
    );
};
