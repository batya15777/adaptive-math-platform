import { useProfile } from "../../contexts/useProfile.js";

// Dual-icon theme switch (sun | moon) bound to ProfileTheme. Reusable across screens.
export function ThemeToggle() {
    const { profileData, updateProfile } = useProfile();
    const isDark = (profileData.theme || "LIGHT") === "DARK";
    const set = (theme) => { if ((theme === "DARK") !== isDark) updateProfile({ theme }).catch(() => {}); };
    return (
        <div className="tt" role="group" aria-label="Theme">
            <button type="button" className={"tt-opt" + (!isDark ? " on" : "")} aria-pressed={!isDark} aria-label="Light" onClick={() => set("LIGHT")}>☀️</button>
            <button type="button" className={"tt-opt" + (isDark ? " on" : "")} aria-pressed={isDark} aria-label="Dark" onClick={() => set("DARK")}>🌙</button>
        </div>
    );
}
