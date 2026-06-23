import { useState } from "react";

// Edit basic user details (fullName/email/age/gender).
// Presentational: receives the user + strings via props, hands values back via onSubmit.
// The parent remounts this via key={user.id}, so the initial form is seeded once.
export const UserEditModal = ({ initialUser, onSubmit, onClose, dir = "ltr", t, error }) => {
    const [form, setForm] = useState(() => ({
        fullName: initialUser?.username ?? "",
        email: initialUser?.email ?? "",
        age: initialUser?.age ?? "",
        gender: initialUser?.gender ?? "male",
    }));

    const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

    const submit = (e) => {
        e.preventDefault();
        onSubmit({
            fullName: form.fullName.trim(),
            email: form.email.trim(),
            age: form.age === "" ? null : Number(form.age),
            gender: form.gender,
        });
    };

    return (
        <div className="adm-overlay" onClick={onClose}>
            <form
                className="adm-modal"
                style={{ direction: dir }}
                onClick={(e) => e.stopPropagation()}
                onSubmit={submit}
            >
                <h3 className="adm-modal-title">✏️ {t.editUserTitle}</h3>

                <div className="adm-field">
                    <label className="adm-field-label">{t.colName}</label>
                    <input className="adm-field-input" value={form.fullName} onChange={set("fullName")} autoFocus />
                </div>
                <div className="adm-field">
                    <label className="adm-field-label">{t.colEmail}</label>
                    <input type="email" className="adm-field-input" value={form.email} onChange={set("email")} />
                </div>
                <div className="adm-field">
                    <label className="adm-field-label">{t.colAge}</label>
                    <input type="number" min="1" max="120" className="adm-field-input" value={form.age} onChange={set("age")} />
                </div>
                <div className="adm-field">
                    <label className="adm-field-label">{t.colGender}</label>
                    <select className="adm-field-select" value={form.gender} onChange={set("gender")}>
                        <option value="male">{t.genderMale}</option>
                        <option value="female">{t.genderFemale}</option>
                    </select>
                </div>

                {error && <div className="adm-notice adm-notice--error" style={{ marginTop: 0 }}>⚠ {error}</div>}

                <div className="adm-modal-actions">
                    <button type="button" className="adm-btn adm-btn--ghost" onClick={onClose}>{t.cancel}</button>
                    <button type="submit" className="adm-btn adm-btn--primary">{t.save}</button>
                </div>
            </form>
        </div>
    );
};
