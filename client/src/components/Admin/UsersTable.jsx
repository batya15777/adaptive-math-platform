// Presentational table for the admin user list. Receives data + strings (t) +
// locale via props only — no data fetching, state, or i18n lookups — so it stays
// reusable and easy to test.
export const UsersTable = ({ users, t, locale, onChangeRole, onSetStatus, onEdit, currentUserId }) => (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
            <tr>
                {[t.colId, t.colName, t.colEmail, t.colRole, t.colStatus, t.colAge, t.colGender, t.colStars, t.colCreated, t.colActions].map((h, i) => (
                    <th key={i} style={th}>{h}</th>
                ))}
            </tr>
        </thead>
        <tbody>
            {users.map((u) => {
                const status = u.accountStatus;
                const deleted = status === "DELETED";
                const statusLabel = deleted ? t.userStatusDeleted : status === "BLOCKED" ? t.userStatusBlocked : t.userStatusActive;
                return (
                    <tr key={u.id}>
                        <td style={cell}>{u.id}</td>
                        <td style={cell}>{u.username}</td>
                        <td style={cell}>{u.email}</td>
                        <td style={cell}><span style={roleBadge(u.role)}>{u.role}</span></td>
                        <td style={cell}>
                            <span style={statusBadge(status)}>{statusLabel}</span>
                        </td>
                        <td style={cell}>{u.age ?? "—"}</td>
                        <td style={cell}>{u.gender ?? "—"}</td>
                        <td style={cell}>⭐ {u.totalStars ?? 0}</td>
                        <td style={cell}>{u.createdAt ? new Date(u.createdAt).toLocaleString(locale) : "—"}</td>
                        <td style={cell}>
                            {u.id === currentUserId ? (
                                <span style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                                    <span style={selfTag}>{t.currentAccount}</span>
                                    <button onClick={() => onEdit(u)}>{t.editUser}</button>
                                </span>
                            ) : deleted ? (
                                <button onClick={() => onSetStatus(u, "ACTIVE")}>{t.restore}</button>
                            ) : (
                                <span style={{ display: "inline-flex", gap: 6 }}>
                                    {u.role === "ADMIN" ? (
                                        <button onClick={() => onChangeRole(u, "STUDENT")}>{t.makeStudent}</button>
                                    ) : (
                                        <button onClick={() => onChangeRole(u, "ADMIN")}>{t.makeAdmin}</button>
                                    )}
                                    {status === "BLOCKED" ? (
                                        <button onClick={() => onSetStatus(u, "ACTIVE")}>{t.unblock}</button>
                                    ) : (
                                        <button onClick={() => onSetStatus(u, "BLOCKED")}>{t.block}</button>
                                    )}
                                    <button onClick={() => onSetStatus(u, "DELETED")}>{t.softDelete}</button>
                                    <button onClick={() => onEdit(u)}>{t.editUser}</button>
                                </span>
                            )}
                        </td>
                    </tr>
                );
            })}
        </tbody>
    </table>
);

const th = { textAlign: "start", borderBottom: "2px solid #ddd", padding: 8 };
const cell = { borderBottom: "1px solid #eee", padding: 8 };
const roleBadge = (role) => ({
    padding: "2px 8px", borderRadius: 12, fontSize: 12, fontWeight: 700,
    color: "#fff", background: role === "ADMIN" ? "#aa3bff" : "#6c757d",
});
const selfTag = { fontSize: 12, color: "#6c757d" };
const statusBadge = (status) => ({
    padding: "2px 8px", borderRadius: 12, fontSize: 12, fontWeight: 700, color: "#fff",
    background: status === "DELETED" ? "#6c757d" : status === "BLOCKED" ? "#dc3545" : "#28a745",
});
