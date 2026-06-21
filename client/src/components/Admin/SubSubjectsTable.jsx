// Presentational sub-subjects table (mirrors SubjectsTable). Props only, no delete.
// UI guards (also enforced by the backend):
//  - "Publish" disabled when the sub-subject has no question templates.
//  - Protected sub-subject (parent subject protected, system=true): edit + disable blocked.
//  - "Disable" blocked for the last published sub-subject of an active subject.
export const SubSubjectsTable = ({ subSubjects, subjectActive, activeSubSubjectCount, onEdit, onToggle, t }) => (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
            <tr>
                {[t.colId, t.colName, t.colStatus, t.colTemplates, t.colActions].map((h, i) => (
                    <th key={i} style={th}>{h}</th>
                ))}
            </tr>
        </thead>
        <tbody>
            {subSubjects.map((ss) => {
                const isLastActive = subjectActive && ss.active && activeSubSubjectCount <= 1;
                const disableBlocked = ss.system || isLastActive;
                return (
                    <tr key={ss.id}>
                        <td style={cell}>{ss.id}</td>
                        <td style={cell}>{ss.name}</td>
                        <td style={cell}>
                            <span style={statusBadge(ss.active)}>
                                {ss.active ? t.statusActive : t.statusDraft}
                            </span>
                        </td>
                        <td style={cell}>{ss.templateCount}</td>
                        <td style={cell}>
                            <button
                                onClick={() => onEdit(ss)}
                                disabled={ss.system}
                                title={ss.system ? t.protectedSubSubjectNote : ""}
                            >
                                {t.edit}
                            </button>{" "}
                            {ss.active ? (
                                <button
                                    onClick={() => onToggle(ss)}
                                    disabled={disableBlocked}
                                    title={ss.system ? t.protectedSubSubjectNote : (isLastActive ? t.lastActiveSubSubject : "")}
                                >
                                    {t.disable}
                                </button>
                            ) : (
                                <button
                                    onClick={() => onToggle(ss)}
                                    disabled={ss.templateCount === 0}
                                    title={ss.templateCount === 0 ? t.cannotPublishNoTemplates : ""}
                                >
                                    {t.publish}
                                </button>
                            )}
                            {ss.system && (
                                <div style={lockNote}>🔒 {t.protectedSubSubjectNote}</div>
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
