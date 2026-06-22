import { useState, useRef, useEffect } from "react";
import "./ThemedSelect.css";

// Reusable themed dropdown — a styled replacement for the native <select> so the
// option list matches the app theme (dark/light) instead of the OS popup.
// Props:
//   value        current value (string|number)
//   onChange     (value) => void
//   options      [{ value, label }]
//   placeholder  shown when nothing is selected
//   icon         optional leading glyph
//   disabled, ariaLabel
// Closes on Esc / outside-click, keyboard-selectable, auto-flips up near the bottom.
export function ThemedSelect({ value, onChange, options = [], placeholder = "", icon, ariaLabel, disabled }) {
    const [open, setOpen] = useState(false);
    const [dropUp, setDropUp] = useState(false);
    const wrapRef = useRef(null);

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

    const toggle = () => {
        if (disabled) return;
        if (!open && wrapRef.current) {
            const r = wrapRef.current.getBoundingClientRect();
            setDropUp(window.innerHeight - r.bottom < 290);
        }
        setOpen((o) => !o);
    };

    const choose = (v) => { onChange(v); setOpen(false); };
    const selected = options.find((o) => String(o.value) === String(value));

    return (
        <div className="ts-wrap" ref={wrapRef}>
            <button
                type="button"
                className={"ts-btn" + (selected ? "" : " is-placeholder")}
                disabled={disabled}
                aria-haspopup="listbox"
                aria-expanded={open}
                aria-label={ariaLabel}
                onClick={toggle}
            >
                {icon && <span className="ts-ic" aria-hidden="true">{icon}</span>}
                <span className="ts-val">{selected ? selected.label : placeholder}</span>
                <span className="ts-caret" aria-hidden="true">▾</span>
            </button>
            {open && (
                <ul className={"ts-menu" + (dropUp ? " ts-menu--up" : "")} role="listbox" aria-label={ariaLabel}>
                    {options.map((o) => {
                        const isSel = String(o.value) === String(value);
                        return (
                            <li
                                key={o.value}
                                role="option"
                                aria-selected={isSel}
                                tabIndex={0}
                                className={"ts-item" + (isSel ? " is-sel" : "")}
                                onClick={() => choose(o.value)}
                                onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); choose(o.value); } }}
                            >
                                <span>{o.label}</span>
                                {isSel && <span className="ts-check" aria-hidden="true">✓</span>}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
}
