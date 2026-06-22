import { useNavigate } from "react-router-dom";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getAuthStrings } from "./authStrings.js";
import { MathGalaxyLogo } from "../ui/MathGalaxyLogo.jsx";
import "./auth.css";

// Logo splash. Clicking anywhere goes straight to login.
export function Landing() {
    const navigate = useNavigate();
    const { dir, language } = useLanguage();
    const t = getAuthStrings(language);
    const go = () => navigate("/login");

    return (
        <div
            className="mg-auth mg-land"
            data-theme="dark"
            dir={dir}
            role="button"
            tabIndex={0}
            aria-label={t.clickToStart}
            onClick={go}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") go(); }}
        >
            <div className="mg-land-center">
                <div className="mg-land-logo">
                    <MathGalaxyLogo size="xl" />
                </div>
            </div>

            <div className="mg-land-start">
                <span>{t.clickToStart}</span>
                <svg className="mg-land-chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M6 9l6 6 6-6" />
                </svg>
            </div>
        </div>
    );
}
