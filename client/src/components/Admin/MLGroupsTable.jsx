// Presentational, read-only table of ML cluster groups. Props only — no actions.
// Null-safe: avgAccuracy / topErrors / lastAssignedAt may be null → render "—".
const DASH = "—";

export const MLGroupsTable = ({ groups, t, locale }) => (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
            <tr>
                {[t.colClusterId, t.colLabel, t.colMembers, t.colAvgAccuracy, t.colTopErrors, t.colModelK, t.colLastRun]
                    .map((h, i) => <th key={i} style={th}>{h}</th>)}
            </tr>
        </thead>
        <tbody>
            {groups.map((g) => (
                <tr key={g.clusterId}>
                    <td style={cell}>{g.clusterId}</td>
                    <td style={cell}>{g.label || DASH}</td>
                    <td style={cell}>{g.memberCount}</td>
                    <td style={cell}>{g.avgAccuracy != null ? `${Math.round(g.avgAccuracy * 100)}%` : DASH}</td>
                    <td style={cell}>{g.topErrors && g.topErrors.length ? g.topErrors.join(", ") : DASH}</td>
                    <td style={cell}>{g.modelK != null ? g.modelK : DASH}</td>
                    <td style={cell}>{g.lastAssignedAt ? new Date(g.lastAssignedAt).toLocaleString(locale) : DASH}</td>
                </tr>
            ))}
        </tbody>
    </table>
);

const th = { textAlign: "start", borderBottom: "2px solid #ddd", padding: 8 };
const cell = { borderBottom: "1px solid #eee", padding: 8 };
