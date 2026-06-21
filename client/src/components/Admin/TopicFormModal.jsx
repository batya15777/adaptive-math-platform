import { useState } from "react";

// Create/edit a topic. Controlled by the parent via `open`. Purely presentational:
// it owns only the local input value and receives strings (t) + direction via props.
export const TopicFormModal = ({ open, initialName = "", onSubmit, onClose, t, dir = "ltr" }) => {
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
                <h3 style={{ marginTop: 0 }}>{initialName ? t.editTopic : t.newTopic}</h3>
                <input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder={t.topicNamePlaceholder}
                    autoFocus
                    style={{ width: "100%", padding: 10, boxSizing: "border-box", marginBottom: 16 }}
                />
                <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
                    <button type="button" onClick={onClose}>{t.cancel}</button>
                    <button type="submit" disabled={!name.trim()} style={saveBtn}>{t.save}</button>
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
