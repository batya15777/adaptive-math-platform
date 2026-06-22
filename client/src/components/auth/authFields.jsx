import { useRef, useState, useEffect } from "react";

// Shared presentational atoms for the auth screens. Purely visual — they hold no
// business logic, so Login/Register keep their existing state, validation and API calls.

// A labelled input shell with optional leading icon + trailing slot.
export function Field({ label, icon, children, trailing, error }) {
    return (
        <label className="mg-fld">
            <span className="mg-lbl">{label}</span>
            <span className={"mg-inp" + (error ? " mg-inp--err" : "")}>
                {icon && <span className="mg-ic" aria-hidden="true">{icon}</span>}
                {children}
                {trailing}
            </span>
        </label>
    );
}

// Clean line-art eye icon. `off` adds the diagonal slash (password hidden).
function EyeIcon({ off }) {
    return (
        <svg className="mg-eye-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M1.5 12S5.5 5 12 5s10.5 7 10.5 7-4 7-10.5 7S1.5 12 1.5 12Z" />
            <circle cx="12" cy="12" r="3" />
            {off && <line x1="3.2" y1="3.2" x2="20.8" y2="20.8" />}
        </svg>
    );
}

// Password field with a show/hide toggle. `labels` = { show, hide } for a11y/i18n.
// Visible password → open eye; hidden password → eye with a slash.
export function PasswordField({ label, value, onChange, placeholder, show, onToggle, labels, autoComplete }) {
    return (
        <Field
            label={label}
            icon="🔒"
            trailing={
                <button
                    type="button"
                    className="mg-eye"
                    onClick={onToggle}
                    aria-pressed={show}
                    aria-label={show ? labels.hide : labels.show}
                >
                    <EyeIcon off={!show} />
                </button>
            }
        >
            <input
                className="mg-field-input"
                type={show ? "text" : "password"}
                value={value}
                placeholder={placeholder}
                onChange={onChange}
                autoComplete={autoComplete}
            />
        </Field>
    );
}

// Themed age picker (replaces the native <select> so the dropdown matches dark/light).
// `groups` = [{ label, from, to }]. Emits the chosen age as a string via onChange.
// Auto-flips upward when there isn't room below (it sits low in the Register card).
export function AgeSelect({ label, value, placeholder, groups, onChange }) {
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
        if (!open && wrapRef.current) {
            const r = wrapRef.current.getBoundingClientRect();
            setDropUp(window.innerHeight - r.bottom < 340);
        }
        setOpen((o) => !o);
    };

    const choose = (n) => { onChange(String(n)); setOpen(false); };
    const selected = value === "" || value == null ? null : Number(value);

    return (
        <div className="mg-fld mg-agewrap" ref={wrapRef}>
            <span className="mg-lbl">{label}</span>
            <button
                type="button"
                className={"mg-inp mg-agebtn" + (selected == null ? " is-placeholder" : "")}
                aria-haspopup="listbox"
                aria-expanded={open}
                onClick={toggle}
            >
                <span className="mg-ic" aria-hidden="true">📅</span>
                <span className="mg-ageval">{selected == null ? placeholder : selected}</span>
                <span className="mg-caret" aria-hidden="true">▾</span>
            </button>
            {open && (
                <div className={"mg-agemenu" + (dropUp ? " mg-agemenu--up" : "")} role="listbox" aria-label={label}>
                    {groups.map((g) => (
                        <div className="mg-agegroup" key={g.label}>
                            <div className="mg-agehead">{g.label}</div>
                            <div className="mg-agegrid">
                                {Array.from({ length: g.to - g.from + 1 }, (_, i) => g.from + i).map((n) => (
                                    <button
                                        key={n}
                                        type="button"
                                        role="option"
                                        aria-selected={n === selected}
                                        className={"mg-ageitem" + (n === selected ? " sel" : "")}
                                        onClick={() => choose(n)}
                                    >
                                        {n}
                                    </button>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

// 6-box numeric OTP. Always LTR. Auto-advances, backspaces, and accepts a full paste.
// Emits the joined string via onChange so the parent keeps a single `code` value.
export function Otp({ length = 6, value = "", onChange, ariaLabel = "Verification code" }) {
    const refs = useRef([]);
    const digits = Array.from({ length }, (_, i) => value[i] || "");

    const setAt = (i, d) => {
        const arr = digits.slice();
        arr[i] = d;
        onChange(arr.join(""));
    };
    const handleInput = (i, e) => {
        const d = e.target.value.replace(/\D/g, "").slice(-1);
        setAt(i, d);
        if (d && i < length - 1) refs.current[i + 1]?.focus();
    };
    const handleKey = (i, e) => {
        if (e.key === "Backspace" && !digits[i] && i > 0) {
            refs.current[i - 1]?.focus();
        } else if (e.key === "ArrowLeft" && i > 0) {
            refs.current[i - 1]?.focus();
        } else if (e.key === "ArrowRight" && i < length - 1) {
            refs.current[i + 1]?.focus();
        }
    };
    const handlePaste = (e) => {
        const txt = (e.clipboardData.getData("text") || "").replace(/\D/g, "").slice(0, length);
        if (!txt) return;
        e.preventDefault();
        onChange(txt);
        refs.current[Math.min(txt.length, length) - 1]?.focus();
    };

    return (
        <div className="mg-otp" dir="ltr" role="group" aria-label={ariaLabel} onPaste={handlePaste}>
            {Array.from({ length }).map((_, i) => (
                <input
                    key={i}
                    ref={(el) => (refs.current[i] = el)}
                    className="mg-otp-box"
                    inputMode="numeric"
                    autoComplete={i === 0 ? "one-time-code" : "off"}
                    maxLength={1}
                    value={digits[i]}
                    onChange={(e) => handleInput(i, e)}
                    onKeyDown={(e) => handleKey(i, e)}
                    aria-label={`Digit ${i + 1}`}
                />
            ))}
        </div>
    );
}
