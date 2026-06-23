import { useState, useRef, useEffect, useCallback } from "react";

/**
 * Custom styled dropdown that replaces the native <select> in the admin toolbar.
 *
 * Props:
 *   label   — text shown before the button, e.g. "Status"
 *   value   — currently selected option value
 *   onChange(value) — called when user picks a new option
 *   options — [{ value: string, label: string }, ...]
 */
export const FilterDropdown = ({ label, value, onChange, options }) => {
    const [open, setOpen] = useState(false);
    const containerRef = useRef(null);

    const selectedLabel = options.find((o) => o.value === value)?.label ?? value;

    // Close on outside click
    useEffect(() => {
        if (!open) return;
        const handler = (e) => {
            if (containerRef.current && !containerRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, [open]);

    // Close on Escape
    const handleKeyDown = useCallback((e) => {
        if (e.key === "Escape") setOpen(false);
    }, []);

    const handleSelect = (val) => {
        onChange(val);
        setOpen(false);
    };

    return (
        <div className="adm-filter-group" ref={containerRef} onKeyDown={handleKeyDown}>
            <span className="adm-filter-label">{label}:</span>
            <button
                type="button"
                className={"adm-dropdown-btn" + (open ? " open" : "")}
                onClick={() => setOpen((o) => !o)}
                aria-haspopup="listbox"
                aria-expanded={open}
            >
                <span>{selectedLabel}</span>
                <svg
                    className={"adm-dropdown-caret" + (open ? " open" : "")}
                    width="10" height="10" viewBox="0 0 10 10"
                    fill="none" aria-hidden="true"
                >
                    <path d="M2 3.5L5 6.5L8 3.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
            </button>

            {open && (
                <ul
                    className="adm-dropdown-menu"
                    role="listbox"
                    aria-label={label}
                >
                    {options.map((o) => {
                        const isSelected = o.value === value;
                        return (
                            <li
                                key={o.value}
                                role="option"
                                aria-selected={isSelected}
                                className={"adm-dropdown-item" + (isSelected ? " selected" : "")}
                                onClick={() => handleSelect(o.value)}
                            >
                                <span className="adm-dropdown-check" aria-hidden="true">
                                    {isSelected ? "✓" : ""}
                                </span>
                                {o.label}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
};
