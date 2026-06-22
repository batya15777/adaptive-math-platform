import { useContext } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useProfile } from "../../contexts/useProfile.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getNavStrings } from "../navStrings.js";
import { logout as logoutApi } from "../../service/authApi.js";
import { MathGalaxyLogo } from "./MathGalaxyLogo.jsx";
import { ThemeToggle } from "./ThemeToggle.jsx";
import { ThemedSelect } from "./ThemedSelect.jsx";
import "./appTopBar.css";

// Themed app top bar for in-app screens: language + theme toggle + logout (start),
// MathGalaxy logo (end). Reuses the existing profile/auth logic (no new state).
export function AppTopBar() {
    const { logoutUser } = useContext(AuthContext);
    const { profileData, options, updateProfile, loading } = useProfile();
    const { language } = useLanguage();
    const nav = getNavStrings(language);
    const navigate = useNavigate();

    const langs = options.languages?.length
        ? options.languages
        : [{ value: profileData.language, label: profileData.language }];

    const doLogout = () => {
        logoutApi().then(() => { logoutUser(); navigate("/login"); }).catch(() => {});
    };

    return (
        <header className="appbar">
            <Link to="/home" className="appbar-logo" aria-label="MathGalaxy"><MathGalaxyLogo size="md" /></Link>
            <div className="appbar-controls">
                <ThemeToggle />
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
                <button type="button" className="appbar-logout" onClick={doLogout}>{nav.logout}</button>
            </div>
        </header>
    );
}
