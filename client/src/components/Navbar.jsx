import { useContext } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { AuthContext } from "../context/AuthContextSetup.js";
import { logout } from "../service/authApi.js";
import { useLanguage } from "../i18n/useLanguage.js";
import { getNavStrings } from "./navStrings.js";
import { LanguageSwitcher } from "./LanguageSwitcher/LanguageSwitcher.jsx";

function Navbar() {

    const { user, logoutUser } = useContext(AuthContext);
    const navigate = useNavigate();
    const location = useLocation();
    const { language, dir } = useLanguage();
    const t = getNavStrings(language);

    // Hide the language switcher during an active exercise: the quiz generates
    // questions in the active language, so changing it mid-question is disruptive.
    const isExerciseScreen = /\/math-training\/[^/]+\/play$/.test(location.pathname);

    // Pages that render their own themed top bar (AppTopBar) — hide this grey navbar there.
    // Admin pages have a full sidebar layout with their own controls. The exercise screen
    // (/math-training/:id/play) also carries its own AppTopBar, so the legacy grey strip
    // must not show over its space background.
    const isAdminPage = location.pathname.startsWith('/admin');
    const isGamesPage = location.pathname.startsWith('/games');
    const hasOwnTopBar = isAdminPage || isGamesPage || isExerciseScreen || ['/home', '/leaderboard', '/daily-practice', '/dashboard', '/math-training', '/profile-settings'].includes(location.pathname);

    const handleLogout = () => {
        logout()
            .then(() => {
                logoutUser();
                navigate("/login"); // Redirect to login after logout
            })
            .catch(error => {
                console.log("שגיאה בהתנתקות:", error);
            });
    };

    if (!user || hasOwnTopBar) {
        return null;
    }


    return(
        <nav dir={dir} style={{ padding: "10px", backgroundColor: "#f0f0f0", marginBottom: "20px", display: "flex", alignItems: "center", gap: "16px" }}>
            <span>{t.greeting}, {user.username}!</span>
            <button onClick={handleLogout}>{t.logout}</button>
            {/* Single, app-wide language control — defined once here. Hidden on the
                active exercise screen to avoid changing the language mid-question. */}
            {!isExerciseScreen && (
                <span style={{ marginInlineStart: "auto" }}><LanguageSwitcher /></span>
            )}
        </nav>
    );

}export default Navbar;
