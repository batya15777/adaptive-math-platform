import { useState } from "react";
import TutorChat from "./TutorChat.jsx";
import { getTutorChatStrings } from "./tutorChatStrings.js";
import "./TutorChatLauncher.css";

// Friendly robot face for the floating button.
const RobotIcon = () => (
    <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
        <rect x="10" y="14" width="28" height="23" rx="9" fill="#ffffff" />
        <circle cx="19" cy="25.5" r="3.4" fill="#3a2d6e" />
        <circle cx="29" cy="25.5" r="3.4" fill="#3a2d6e" />
        <circle cx="20.1" cy="24.4" r="1" fill="#9be8ff" />
        <circle cx="30.1" cy="24.4" r="1" fill="#9be8ff" />
        <rect x="22" y="7" width="4" height="6" rx="2" fill="#ffffff" />
        <circle cx="24" cy="6" r="2.6" fill="#9be8ff" />
        <rect x="20" y="31.5" width="8" height="2.2" rx="1.1" fill="#c7b6f2" />
    </svg>
);

// Floating launcher for the existing TutorChat: shows a robot button (closed by default)
// that opens the chat panel on click. Reuses TutorChat as-is — no chat logic duplicated.
export function TutorChatLauncher({ questionId, language }) {
    const [open, setOpen] = useState(false);
    const strings = getTutorChatStrings(language);

    if (open) {
        return (
            <div className="tcl-panel" role="dialog" aria-label={strings.title}>
                <button type="button" className="tcl-close" onClick={() => setOpen(false)} aria-label={strings.close}>✕</button>
                <TutorChat key={questionId} questionId={questionId} language={language} />
            </div>
        );
    }

    return (
        <button type="button" className="tcl-fab" onClick={() => setOpen(true)} title={strings.openHint} aria-label={strings.openHint}>
            <span className="tcl-fab-bot"><RobotIcon /></span>
            <span className="tcl-fab-label">{strings.title}</span>
        </button>
    );
}

export default TutorChatLauncher;
