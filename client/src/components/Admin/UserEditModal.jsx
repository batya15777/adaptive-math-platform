import { useState } from "react";

// Edit basic user details (fullName/email/age/gender). Presentational: receives the
// user + strings via props, hands the values back via onSubmit, and shows the parent's
// error without closing on failure. The parent remounts this via key={user.id}, so the
// initial form is seeded once from initialUser — no effect/render-time setState needed.
export const UserEditModal = ({ initialUser, onSubmit, onClose, dir = "ltr", t, error }) => {
    const [form, setForm] = useState(() => ({
        fullName: initialUser?.username ?? "",   // AdminUserDto.username = fullName
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
        <div style={overlay} onClick={onClose}>
            <form style={{ ...box, direction: dir }} onClick={(e) => e.stopPropagation()} onSubmit={submit}>
                <h3 style={{ marginTop: 0 }}>{t.editUserTitle}</h3>

                <label style={lbl}>{t.colName}
                    <input value={form.fullName} onChange={set("fullName")} style={inp} autoFocus />
                </label>
                <label style={lbl}>{t.colEmail}
                    <input type="email" value={form.email} onChange={set("email")} style={inp} />
                </label>
                <label style={lbl}>{t.colAge}
                    <input type="number" min="1" max="120" value={form.age} onChange={set("age")} style={inp} />
                </label>
                <label style={lbl}>{t.colGender}
                    <select value={form.gender} onChange={set("gender")} style={inp}>
                        <option value="male">{t.genderMale}</option>
                        <option value="female">{t.genderFemale}</option>
                    </select>
                </label>

                {error && <p style={{ color: "#dc3545", marginTop: 4 }}>{error}</p>}

                <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 12 }}>
                    <button type="button" onClick={onClose}>{t.cancel}</button>
                    <button type="submit" style={saveBtn}>{t.save}</button>
                </div>
            </form>
        </div>
    );
};

const overlay = {
    position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)",
    display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000,
};
const box = { background: "#fff", borderRadius: 12, padding: 24, width: "min(440px, 92%)" };
const lbl = { display: "block", fontSize: 13, color: "#444", marginBottom: 10 };
const inp = { width: "100%", padding: 8, boxSizing: "border-box", marginTop: 4 };
const saveBtn = { background: "#aa3bff", color: "#fff", border: 0, padding: "6px 14px", borderRadius: 6 };
