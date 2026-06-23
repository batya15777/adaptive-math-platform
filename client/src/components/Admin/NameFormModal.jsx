import { useState } from "react";

// Generic create/edit-name modal. Reused for Topics, Subjects, Sub-subjects.
// Purely presentational: owns only the local input value; all labels come via props.
export const NameFormModal = ({
    open,
    initialName = "",
    onSubmit,
    onClose,
    dir = "ltr",
    title,
    placeholder,
    saveLabel,
    cancelLabel,
}) => {
    const [name, setName] = useState(initialName);

    const [prevOpen, setPrevOpen] = useState(open);
    if (open !== prevOpen) {
        setPrevOpen(open);
        setName(initialName);
    }

    if (!open) return null;

    const submit = (e) => {
        e.preventDefault();
        if (name.trim()) onSubmit(name.trim());
    };

    return (
        <div className="adm-overlay" onClick={onClose}>
            <form
                className="adm-modal"
                style={{ direction: dir }}
                onClick={(e) => e.stopPropagation()}
                onSubmit={submit}
            >
                <h3 className="adm-modal-title">{title}</h3>
                <div className="adm-field">
                    <input
                        className="adm-field-input"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder={placeholder}
                        autoFocus
                    />
                </div>
                <div className="adm-modal-actions">
                    <button type="button" className="adm-btn adm-btn--ghost" onClick={onClose}>{cancelLabel}</button>
                    <button type="submit" disabled={!name.trim()} className="adm-btn adm-btn--primary">{saveLabel}</button>
                </div>
            </form>
        </div>
    );
};
