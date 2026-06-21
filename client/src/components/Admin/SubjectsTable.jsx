// Presentational subjects table (mirrors TopicsTable). Receives data, callbacks
// and strings (t) via props only. No delete by design.
// UI guards (also enforced by the backend):
//  - "Publish" disabled for a subject with no sub-subjects.
//  - Protected subject ("Calculation", system=true): edit + disable blocked.
//  - "Disable" blocked for the last published subject of an active topic.
export const SubjectsTable = ({ subjects, topicActive, activeSubjectCount, onEdit, onToggle, onManageSubSubjects, t }) => (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
            <tr>
                {[t.colId, t.colName, t.colStatus, t.colSubSubjects, t.colActions].map((h, i) => (
                    <th key={i} style={th}>{h}</th>
                ))}
            </tr>
        </thead>
        <tbody>
            {subjects.map((s) => {
                const isLastActive = topicActive && s.active && activeSubjectCount <= 1;
                const disableBlocked = s.system || isLastActive;
                return (
                    <tr key={s.id}>
                        <td style={cell}>{s.id}</td>
                        <td style={cell}>{s.name}</td>
                        <td style={cell}>
                            <span style={statusBadge(s.active)}>
                                {s.active ? t.statusActive : t.statusDraft}
                            </span>
                        </td>
                        <td style={cell}>{s.subSubjectCount}</td>
                        <td style={cell}>
                            <button onClick={() => onManageSubSubjects(s)}>{t.manageSubSubjects}</button>{" "}
                            <button
                                onClick={() => onEdit(s)}
                                disabled={s.system}
                                title={s.system ? t.protectedSubjectNote : ""}
                            >
                                {t.edit}
                            </button>{" "}
                            {s.active ? (
                                <button
                                    onClick={() => onToggle(s)}
                                    disabled={disableBlocked}
                                    title={s.system ? t.protectedSubjectNote : (isLastActive ? t.lastActiveSubject : "")}
                                >
                                    {t.disable}
                                </button>
                            ) : (
                                <button
                                    onClick={() => onToggle(s)}
                                    disabled={s.activeSubSubjectCount === 0}
                                    title={s.activeSubSubjectCount === 0 ? t.cannotPublishSubjectEmpty : ""}
                                >
                                    {t.publish}
                                </button>
                            )}
                            {s.system && (
                                <div style={lockNote}>🔒 {t.protectedSubjectNote}</div>
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
const statusBadge = (active) => ({
    padding: "2px 8px", borderRadius: 12, fontSize: 12, fontWeight: 700,
    color: "#fff", background: active ? "#28a745" : "#6c757d",
});
const lockNote = { marginTop: 4, fontSize: 12, color: "#6c757d" };
