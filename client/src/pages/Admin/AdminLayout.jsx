import { useContext } from "react";
import { Outlet, NavLink, useNavigate } from "react-router-dom";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { useProfile } from "../../contexts/useProfile.js";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { getNavStrings } from "../../components/navStrings.js";
import { logout as logoutApi } from "../../service/authApi.js";
import { MathGalaxyLogo } from "../../components/ui/MathGalaxyLogo.jsx";
import { ThemedSelect } from "../../components/ui/ThemedSelect.jsx";
import "./admin.css";

const NAV_ITEMS = [
    { to: "/admin/dashboard", icon: "📊", key: "dashboardTitle" },
    { to: "/admin/users",     icon: "👥", key: "usersTitle" },
    { to: "/admin/topics",    icon: "📚", key: "topicsTitle" },
    { to: "/admin/ml-groups", icon: "🤖", key: "mlGroupsTitle" },
];

export const AdminLayout = () => {
    const { user, logoutUser } = useContext(AuthContext);
    const { language, dir } = useLanguage();
    const { profileData, options, updateProfile, loading: profileLoading } = useProfile();
    const navigate = useNavigate();
    const t = getAdminStrings(language);
    const nav = getNavStrings(language);

    const isDark = (profileData.theme || "LIGHT") === "DARK";
    const theme = isDark ? "dark" : "light";

    const langs = options.languages?.length
        ? options.languages
        : [{ value: profileData.language, label: profileData.language }];

    const doLogout = () => {
        logoutApi().then(() => { logoutUser(); navigate("/login"); }).catch(() => {});
    };

    const doTheme = (next) => {
        if ((next === "DARK") !== isDark) updateProfile({ theme: next }).catch(() => {});
    };

    const initials = (user?.username || "A").slice(0, 2).toUpperCase();

    return (
        <div className="admin-shell" data-theme={theme}>
            {/* ── Top Navigation Bar ─────────────────────────────────────── */}
            <header className="adm-topnav">
                <div className="adm-topnav-inner" dir={dir}>
                    {/* Brand */}
                    <div className="adm-topnav-brand">
                        <MathGalaxyLogo size="sm" />
                        <span className="adm-topnav-badge">Admin</span>
                    </div>

                    {/* Nav links */}
                    <nav className="adm-topnav-links" aria-label="Admin navigation">
                        {NAV_ITEMS.map(({ to, icon, key }) => (
                            <NavLink
                                key={to}
                                to={to}
                                className={({ isActive }) => "adm-topnav-link" + (isActive ? " active" : "")}
                            >
                                <span className="adm-topnav-link-icon">{icon}</span>
                                <span>{t[key]}</span>
                            </NavLink>
                        ))}
                    </nav>

                    {/* Controls */}
                    <div className="adm-topnav-controls">
                        <ThemedSelect
                            value={profileData.language}
                            disabled={profileLoading}
                            onChange={(v) => { if (v !== profileData.language) updateProfile({ language: v }).catch(() => {}); }}
                            options={langs}
                            icon="🌐"
                            ariaLabel="Language"
                        />
                        <div className="adm-theme-toggle" role="group" aria-label="Theme">
                            <button
                                type="button"
                                className={"adm-theme-btn" + (!isDark ? " active" : "")}
                                onClick={() => doTheme("LIGHT")}
                                title="Light mode"
                            >☀️</button>
                            <button
                                type="button"
                                className={"adm-theme-btn" + (isDark ? " active" : "")}
                                onClick={() => doTheme("DARK")}
                                title="Dark mode"
                            >🌙</button>
                        </div>
                        <div className="adm-user-pill">
                            <div className="adm-user-avatar">{initials}</div>
                            <span>{user?.username}</span>
                        </div>
                        <button type="button" className="adm-btn adm-btn--ghost adm-btn--sm" onClick={doLogout}>
                            ↩ {nav.logout}
                        </button>
                    </div>
                </div>
            </header>

            {/* ── Page Body ──────────────────────────────────────────────── */}
            <div className="adm-page-body" dir={dir}>
                <div className="adm-content">
                    <Outlet />
                </div>
            </div>
        </div>
    );
};
