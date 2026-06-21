import { useState } from "react";

// Generic create/edit-name modal. Reused for Topics and Subjects. Purely
// presentational: owns only the local input value; all labels come via props.
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

    // Reset the input when the modal (re)opens — adjusting state during render is the
    // React-recommended alternative to a setState-in-effect for "derive from props".
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
        <div style={overlay} onClick={onClose}>
            <form style={{ ...box, direction: dir }} onClick={(e) => e.stopPropagation()} onSubmit={submit}>
                <h3 style={{ marginTop: 0 }}>{title}</h3>
                <input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder={placeholder}
                    autoFocus
                    style={{ width: "100%", padding: 10, boxSizing: "border-box", marginBottom: 16 }}
                />
                <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
                    <button type="button" onClick={onClose}>{cancelLabel}</button>
                    <button type="submit" disabled={!name.trim()} style={saveBtn}>{saveLabel}</button>
                </div>
            </form>
        </div>
    );
};

const overlay = {
    position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)",
    display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000,
};
const box = {
    background: "#fff", borderRadius: 12, padding: 24, width: "min(420px, 90%)",
};
const saveBtn = {
    background: "#aa3bff", color: "#fff", border: 0,
    padding: "6px 14px", borderRadius: 6,
};
