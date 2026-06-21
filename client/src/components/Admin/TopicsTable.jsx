// Presentational topics table. Receives data, callbacks and strings (t) via props
// only. There is no delete action by design — topics are published/disabled, not
// removed. "Publish" is disabled for empty topics (a topic with no subjects must
// not reach students).
export const TopicsTable = ({ topics, onEdit, onToggle, t }) => (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
            <tr>
                {[t.colId, t.colName, t.colStatus, t.colSubjects, t.colActions].map((h, i) => (
                    <th key={i} style={th}>{h}</th>
                ))}
            </tr>
        </thead>
        <tbody>
            {topics.map((topic) => (
                <tr key={topic.id}>
                    <td style={cell}>{topic.id}</td>
                    <td style={cell}>{topic.name}</td>
                    <td style={cell}>
                        <span style={statusBadge(topic.active)}>
                            {topic.active ? t.statusActive : t.statusDraft}
                        </span>
                    </td>
                    <td style={cell}>{topic.subjectCount}</td>
                    <td style={cell}>
                        <button onClick={() => onEdit(topic)}>{t.edit}</button>{" "}
                        {topic.active ? (
                            <button onClick={() => onToggle(topic)}>{t.disable}</button>
                        ) : (
                            <button
                                onClick={() => onToggle(topic)}
                                disabled={topic.subjectCount === 0}
                                title={topic.subjectCount === 0 ? t.cannotPublishEmpty : ""}
                            >
                                {t.publish}
                            </button>
                        )}
                    </td>
                </tr>
            ))}
        </tbody>
    </table>
);

const th = { textAlign: "start", borderBottom: "2px solid #ddd", padding: 8 };
const cell = { borderBottom: "1px solid #eee", padding: 8 };
const statusBadge = (active) => ({
    padding: "2px 8px", borderRadius: 12, fontSize: 12, fontWeight: 700,
    color: "#fff", background: active ? "#28a745" : "#6c757d",
});
