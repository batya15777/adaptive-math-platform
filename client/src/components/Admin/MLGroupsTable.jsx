// Presentational, read-only table of ML cluster groups.
// Null-safe: avgAccuracy / topErrors / lastAssignedAt may be null → render "—".
const DASH = "—";
const CLUSTER_COLORS = ["#7C4DFF", "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#06B6D4", "#EC4899"];

export const MLGroupsTable = ({ groups, t, locale }) => (
    <div className="adm-table-wrap">
        <table className="adm-table">
            <thead>
                <tr>
                    {[t.colClusterId, t.colLabel, t.colMembers, t.colAvgAccuracy, t.colTopErrors, t.colModelK, t.colLastRun]
                        .map((h, i) => <th key={i}>{h}</th>)}
                </tr>
            </thead>
            <tbody>
                {groups.map((g, i) => {
                    const color = CLUSTER_COLORS[i % CLUSTER_COLORS.length];
                    const pct = g.avgAccuracy != null ? Math.round(g.avgAccuracy * 100) : null;
                    return (
                        <tr key={g.clusterId}>
                            <td>
                                <span style={{ display: "inline-flex", alignItems: "center", gap: 7 }}>
                                    <span style={{ width: 8, height: 8, borderRadius: "50%", background: color, flexShrink: 0 }} />
                                    <span style={{ fontWeight: 600 }}>#{g.clusterId}</span>
                                </span>
                            </td>
                            <td style={{ fontWeight: 500 }}>{g.label || DASH}</td>
                            <td>
                                <span style={{ fontWeight: 700, fontSize: 15, color }}>
                                    {g.memberCount}
                                </span>
                                <span style={{ fontSize: 11, color: "var(--adm-txt-muted)", marginInlineStart: 4 }}>
                                    {t.membersUnit}
                                </span>
                            </td>
                            <td>
                                {pct != null ? (
                                    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                                        <div className="adm-pbar" style={{ width: 80, flex: "none" }}>
                                            <div className="adm-pbar-fill" style={{ width: `${pct}%`, background: color }} />
                                        </div>
                                        <span style={{ fontSize: 12.5, fontWeight: 600 }}>{pct}%</span>
                                    </div>
                                ) : DASH}
                            </td>
                            <td style={{ fontSize: 12, color: "var(--adm-txt-2)", maxWidth: 200 }}>
                                {g.topErrors?.length
                                    ? g.topErrors.map((e, ei) => (
                                        <span key={ei} style={{
                                            display: "inline-block", background: "var(--adm-th-bg)",
                                            border: "1px solid var(--adm-bd)", borderRadius: 6,
                                            padding: "2px 7px", fontSize: 11, marginInlineEnd: 4, marginBottom: 2,
                                        }}>{e}</span>
                                    ))
                                    : DASH}
                            </td>
                            <td style={{ color: "var(--adm-txt-2)" }}>{g.modelK != null ? g.modelK : DASH}</td>
                            <td style={{ color: "var(--adm-txt-muted)", fontSize: 12, whiteSpace: "nowrap" }}>
                                {g.lastAssignedAt ? new Date(g.lastAssignedAt).toLocaleString(locale) : DASH}
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    </div>
);
