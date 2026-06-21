// Presentational table for the admin user list. Receives data + strings (t) +
// locale via props only — no data fetching, state, or i18n lookups — so it stays
// reusable and easy to test.
export const UsersTable = ({ users, t, locale }) => (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
            <tr>
                {[t.colId, t.colName, t.colEmail, t.colRole, t.colAge, t.colGender, t.colStars, t.colCreated].map((h, i) => (
                    <th key={i} style={th}>{h}</th>
                ))}
            </tr>
        </thead>
        <tbody>
            {users.map((u) => (
                <tr key={u.id}>
                    <td style={cell}>{u.id}</td>
                    <td style={cell}>{u.username}</td>
                    <td style={cell}>{u.email}</td>
                    <td style={cell}><span style={roleBadge(u.role)}>{u.role}</span></td>
                    <td style={cell}>{u.age ?? "—"}</td>
                    <td style={cell}>{u.gender ?? "—"}</td>
                    <td style={cell}>⭐ {u.totalStars ?? 0}</td>
                    <td style={cell}>{u.createdAt ? new Date(u.createdAt).toLocaleString(locale) : "—"}</td>
                </tr>
            ))}
        </tbody>
    </table>
);

const th = { textAlign: "start", borderBottom: "2px solid #ddd", padding: 8 };
const cell = { borderBottom: "1px solid #eee", padding: 8 };
const roleBadge = (role) => ({
    padding: "2px 8px", borderRadius: 12, fontSize: 12, fontWeight: 700,
    color: "#fff", background: role === "ADMIN" ? "#aa3bff" : "#6c757d",
});
