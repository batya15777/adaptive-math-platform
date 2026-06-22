import { useMemo, useState, useRef, useEffect } from "react";
import { Link } from "react-router-dom";
import { useProfile } from "../../contexts/useProfile.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getAuthStrings } from "./authStrings.js";
import { MathGalaxyLogo } from "../ui/MathGalaxyLogo.jsx";
import "./auth.css";

// Deterministic starfield (stable across re-renders).
function useStars(count = 60) {
    return useMemo(() => {
        let s = 7;
        const rnd = () => (s = (s * 9301 + 49297) % 233280) / 233280;
        return Array.from({ length: count }, () => ({
            left: (rnd() * 100).toFixed(2) + "%",
            top: (rnd() * 100).toFixed(2) + "%",
            lg: rnd() < 0.12,
            o: (0.3 + rnd() * 0.6).toFixed(2),
        }));
    }, [count]);
}

// Polished language dropdown — persists to the profile. Opens below, Esc / outside click close.
function LangMenu() {
    const { profileData, options, updateProfile, loading } = useProfile();
    const [open, setOpen] = useState(false);
    const wrapRef = useRef(null);

    const languages = options.languages?.length
        ? options.languages
        : [{ value: profileData.language, label: profileData.language }];
    const currentLabel = languages.find((l) => l.value === profileData.language)?.label || profileData.language;

    useEffect(() => {
        if (!open) return;
        const onDown = (e) => { if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false); };
        const onKey = (e) => { if (e.key === "Escape") setOpen(false); };
        document.addEventListener("mousedown", onDown);
        document.addEventListener("keydown", onKey);
        return () => {
            document.removeEventListener("mousedown", onDown);
            document.removeEventListener("keydown", onKey);
        };
    }, [open]);

    const choose = (value) => {
        if (value !== profileData.language) updateProfile({ language: value }).catch(() => {});
        setOpen(false);
    };

    return (
        <div className="mg-langwrap" ref={wrapRef}>
            <button type="button" className="mg-langbtn" disabled={loading}
                aria-haspopup="listbox" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
                <span className="mg-gl" aria-hidden="true">🌐</span>
                <span className="mg-langlabel">{currentLabel}</span>
                <span className="mg-chev" aria-hidden="true">▾</span>
            </button>
            {open && (
                <ul className="mg-menu" role="listbox" aria-label="Language">
                    {languages.map((o) => {
                        const selected = o.value === profileData.language;
                        return (
                            <li key={o.value} role="option" aria-selected={selected} tabIndex={0}
                                className={"mg-menu-item" + (selected ? " sel" : "")}
                                onClick={() => choose(o.value)}
                                onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); choose(o.value); } }}>
                                <span>{o.label}</span>
                                {selected && <span className="mg-ck" aria-hidden="true">✓</span>}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
}

// Dual-icon theme switch (sun | moon). Persists via ProfileTheme (guest → localStorage).
function ThemeToggle() {
    const { profileData, updateProfile } = useProfile();
    const isDark = (profileData.theme || "LIGHT") === "DARK";
    const set = (theme) => { if ((theme === "DARK") !== isDark) updateProfile({ theme }).catch(() => {}); };
    return (
        <div className="mg-themeseg" role="group" aria-label="Theme">
            <button type="button" className={"mg-themeopt" + (!isDark ? " on" : "")} aria-pressed={!isDark} aria-label="Light" onClick={() => set("LIGHT")}>☀️</button>
            <button type="button" className={"mg-themeopt" + (isDark ? " on" : "")} aria-pressed={isDark} aria-label="Dark" onClick={() => set("DARK")}>🌙</button>
        </div>
    );
}

// Centered-card shell on a galaxy background. The MathGalaxy logo sits at the top of
// the card (single instance). Theme follows ProfileTheme; direction follows language.
// `wide` widens the card (Register). Layout is centered, so language switching never jumps.
export function AuthLayout({ wide, children }) {
    const { dir, language } = useLanguage();
    const { profileData } = useProfile();
    const stars = useStars();
    const theme = (profileData.theme || "LIGHT").toLowerCase();

    return (
        <div className="mg-auth" data-theme={theme} lang={language}>
            <div className="mg-bg" aria-hidden="true">
                <div className="mg-bg-galaxy" />
                <span className="mg-bg-planet" />
                {stars.map((st, i) => (
                    <i key={i} className={st.lg ? "lg" : ""} style={{ left: st.left, top: st.top, opacity: st.o }} />
                ))}
            </div>

            <div className="mg-floatctrls">
                <LangMenu />
                <ThemeToggle />
            </div>

            <main className="mg-shell">
                <section className={"mg-card" + (wide ? " mg-card--wide" : "")} dir={dir}>
                    <Link to="/login" className="mg-cardlogo" aria-label="MathGalaxy">
                        <MathGalaxyLogo size="md" />
                    </Link>
                    {children}
                </section>
            </main>
        </div>
    );
}
