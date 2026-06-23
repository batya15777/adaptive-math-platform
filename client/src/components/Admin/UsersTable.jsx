// Presentational table for the admin user list. Receives data + strings (t) +
// locale via props only — no data fetching, state, or i18n lookups.
export const UsersTable = ({ users, t, locale, onChangeRole, onSetStatus, onEdit, currentUserId }) => (
    <div className="adm-table-wrap">
        <table className="adm-table">
            <thead>
                <tr>
                    {[t.colId, t.colName, t.colEmail, t.colRole, t.colStatus, t.colAge, t.colGender, t.colStars, t.colCreated, t.colActions].map((h, i) => (
                        <th key={i}>{h}</th>
                    ))}
                </tr>
            </thead>
            <tbody>
                {users.map((u) => {
                    const status = u.accountStatus;
                    const deleted = status === "DELETED";
                    const statusLabel = deleted
                        ? t.userStatusDeleted
                        : status === "BLOCKED"
                        ? t.userStatusBlocked
                        : t.userStatusActive;

                    return (
                        <tr key={u.id}>
                            <td style={{ color: "var(--adm-txt-muted)", fontSize: 12 }}>{u.id}</td>
                            <td style={{ fontWeight: 600 }}>{u.username}</td>
                            <td style={{ color: "var(--adm-txt-2)", fontSize: 12.5 }}>{u.email}</td>
                            <td>
                                <span className={`adm-badge adm-badge--${u.role === "ADMIN" ? "admin" : "student"}`}>
                                    {u.role}
                                </span>
                            </td>
                            <td>
                                <span className={`adm-badge adm-badge--${status === "DELETED" ? "deleted" : status === "BLOCKED" ? "blocked" : "active"}`}>
                                    {statusLabel}
                                </span>
                            </td>
                            <td style={{ color: "var(--adm-txt-2)" }}>{u.age ?? "—"}</td>
                            <td style={{ color: "var(--adm-txt-2)" }}>{u.gender ?? "—"}</td>
                            <td>
                                <span className="adm-stars">⭐ {u.totalStars ?? 0}</span>
                            </td>
                            <td style={{ color: "var(--adm-txt-muted)", fontSize: 12, whiteSpace: "nowrap" }}>
                                {u.createdAt ? new Date(u.createdAt).toLocaleDateString(locale) : "—"}
                            </td>
                            <td>
                                {u.id === currentUserId ? (
                                    <span style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                                        <span className="adm-badge adm-badge--self">{t.currentAccount}</span>
                                        <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onEdit(u)}>{t.editUser}</button>
                                    </span>
                                ) : deleted ? (
                                    <button className="adm-btn adm-btn--success adm-btn--sm" onClick={() => onSetStatus(u, "ACTIVE")}>{t.restore}</button>
                                ) : (
                                    <span style={{ display: "inline-flex", gap: 5, flexWrap: "wrap" }}>
                                        {u.role === "ADMIN" ? (
                                            <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onChangeRole(u, "STUDENT")}>{t.makeStudent}</button>
                                        ) : (
                                            <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onChangeRole(u, "ADMIN")}>{t.makeAdmin}</button>
                                        )}
                                        {status === "BLOCKED" ? (
                                            <button className="adm-btn adm-btn--success adm-btn--sm" onClick={() => onSetStatus(u, "ACTIVE")}>{t.unblock}</button>
                                        ) : (
                                            <button className="adm-btn adm-btn--warning adm-btn--sm" onClick={() => onSetStatus(u, "BLOCKED")}>{t.block}</button>
                                        )}
                                        <button className="adm-btn adm-btn--danger adm-btn--sm" onClick={() => onSetStatus(u, "DELETED")}>{t.softDelete}</button>
                                        <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onEdit(u)}>{t.editUser}</button>
                                    </span>
                                )}
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    </div>
);
