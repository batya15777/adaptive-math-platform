import { useContext } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useProfile } from "../../contexts/useProfile.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getNavStrings } from "../navStrings.js";
import { logout as logoutApi } from "../../service/authApi.js";
import { MathGalaxyLogo } from "./MathGalaxyLogo.jsx";
import { ThemeToggle } from "./ThemeToggle.jsx";
import { ThemedSelect } from "./ThemedSelect.jsx";
import "./appTopBar.css";

// Themed app top bar for in-app screens. Row 1: language + theme + logout (start) and the
// MathGalaxy logo (end) — pinned to physical sides so they never flip with language. Row 2
// (full mode only): the primary glass-pill nav with the active route highlighted.
//   minimal — hide the language + theme controls + the nav (keep logo + logout).
//   backTo  — exercise mode: only a "← back" button + the logo, no controls, no nav.
//   onBack  — custom back handler; keeps the same exercise-mode layout.
export function AppTopBar({ minimal = false, backTo = null, onBack = null }) {
    const { logoutUser } = useContext(AuthContext);
    const { profileData, options, updateProfile, loading } = useProfile();
    const { language } = useLanguage();
    const nav = getNavStrings(language);
    const navigate = useNavigate();

    const isExercise = Boolean(backTo || onBack);
    const showNav = !isExercise && !minimal;

    const langs = options.languages?.length
        ? options.languages
        : [{ value: profileData.language, label: profileData.language }];

    const doLogout = () => {
        logoutApi().then(() => { logoutUser(); navigate("/login"); }).catch(() => {});
    };

    const NAV_ITEMS = [
        { to: "/home",             label: nav.navHome,        icon: "🏠", end: true },
        { to: "/dashboard",        label: nav.navStats,       icon: "📊" },
        { to: "/math-training",    label: nav.navTraining,    icon: "✏️" },
        { to: "/leaderboard",      label: nav.navLeaderboard, icon: "🏆" },
        { to: "/profile-settings", label: nav.navProfile,     icon: "👤" },
    ];

    return (
        <header className="appbar">
            <div className="appbar-bar">
                <Link to="/home" className="appbar-logo" aria-label="MathGalaxy"><MathGalaxyLogo size="md" /></Link>
                <div className="appbar-controls">
                    {isExercise ? (
                        <button type="button" className="appbar-back" onClick={onBack || (() => navigate(backTo))}>← {nav.back}</button>
                    ) : (
                        <>
                            {!minimal && <ThemeToggle />}
                            {!minimal && (
                                <div className="appbar-lang">
                                    <ThemedSelect
                                        value={profileData.language}
                                        disabled={loading}
                                        onChange={(v) => { if (v !== profileData.language) updateProfile({ language: v }).catch(() => {}); }}
                                        options={langs}
                                        icon="🌐"
                                        ariaLabel="Language"
                                    />
                                </div>
                            )}
                            <button type="button" className="appbar-logout" onClick={doLogout}>{nav.logout}</button>
                        </>
                    )}
                </div>
            </div>

            {showNav && (
                <nav className="appbar-nav" aria-label={nav.navAria}>
                    {NAV_ITEMS.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) => "appbar-nav-item" + (isActive ? " is-active" : "")}
                        >
                            <span className="appbar-nav-ic" aria-hidden="true">{item.icon}</span>
                            <span className="appbar-nav-label">{item.label}</span>
                        </NavLink>
                    ))}
                </nav>
            )}
        </header>
    );
}
