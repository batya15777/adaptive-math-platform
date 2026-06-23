// Presentational sub-subjects table. Props only, no delete.
// UI guards (also enforced by the backend):
//  - "Publish" disabled when the sub-subject has no question templates.
//  - Protected sub-subject: edit + disable blocked.
//  - "Disable" blocked for the last published sub-subject of an active subject.
export const SubSubjectsTable = ({ subSubjects, subjectActive, activeSubSubjectCount, onEdit, onToggle, t }) => (
    <div className="adm-table-wrap">
        <table className="adm-table">
            <thead>
                <tr>
                    {[t.colId, t.colName, t.colStatus, t.colTemplates, t.colActions].map((h, i) => (
                        <th key={i}>{h}</th>
                    ))}
                </tr>
            </thead>
            <tbody>
                {subSubjects.map((ss) => {
                    const isLastActive = subjectActive && ss.active && activeSubSubjectCount <= 1;
                    const disableBlocked = ss.system || isLastActive;
                    return (
                        <tr key={ss.id}>
                            <td style={{ color: "var(--adm-txt-muted)", fontSize: 12 }}>{ss.id}</td>
                            <td>
                                <span style={{ fontWeight: 600 }}>{ss.name}</span>
                                {ss.system && (
                                    <span style={{ marginInlineStart: 6, fontSize: 11, color: "var(--adm-txt-muted)" }}>🔒</span>
                                )}
                            </td>
                            <td>
                                <span className={`adm-badge adm-badge--${ss.active ? "active-topic" : "draft"}`}>
                                    {ss.active ? t.statusActive : t.statusDraft}
                                </span>
                            </td>
                            <td style={{ color: "var(--adm-txt-2)" }}>{ss.templateCount}</td>
                            <td>
                                <span style={{ display: "inline-flex", gap: 5, flexWrap: "wrap" }}>
                                    <button
                                        className="adm-btn adm-btn--ghost adm-btn--sm"
                                        onClick={() => onEdit(ss)}
                                        disabled={ss.system}
                                        title={ss.system ? t.protectedSubSubjectNote : ""}
                                    >
                                        ✏️ {t.edit}
                                    </button>
                                    {ss.active ? (
                                        <button
                                            className="adm-btn adm-btn--warning adm-btn--sm"
                                            onClick={() => onToggle(ss)}
                                            disabled={disableBlocked}
                                            title={ss.system ? t.protectedSubSubjectNote : (isLastActive ? t.lastActiveSubSubject : "")}
                                        >
                                            {t.disable}
                                        </button>
                                    ) : (
                                        <button
                                            className="adm-btn adm-btn--success adm-btn--sm"
                                            onClick={() => onToggle(ss)}
                                            disabled={ss.templateCount === 0}
                                            title={ss.templateCount === 0 ? t.cannotPublishNoTemplates : ""}
                                        >
                                            ✓ {t.publish}
                                        </button>
                                    )}
                                </span>
                                {ss.system && (
                                    <div style={{ marginTop: 4, fontSize: 11, color: "var(--adm-txt-muted)" }}>
                                        🔒 {t.protectedSubSubjectNote}
                                    </div>
                                )}
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    </div>
);
