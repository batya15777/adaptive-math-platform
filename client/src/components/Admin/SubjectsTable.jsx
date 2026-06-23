// Presentational subjects table. Props only, no delete.
// UI guards (also enforced by the backend):
//  - "Publish" disabled for a subject with no sub-subjects.
//  - Protected system subject: edit + disable blocked.
//  - "Disable" blocked for the last published subject of an active topic.
export const SubjectsTable = ({ subjects, topicActive, activeSubjectCount, onEdit, onToggle, onManageSubSubjects, t }) => (
    <div className="adm-table-wrap">
        <table className="adm-table">
            <thead>
                <tr>
                    {[t.colId, t.colName, t.colStatus, t.colSubSubjects, t.colActions].map((h, i) => (
                        <th key={i}>{h}</th>
                    ))}
                </tr>
            </thead>
            <tbody>
                {subjects.map((s) => {
                    const isLastActive = topicActive && s.active && activeSubjectCount <= 1;
                    const disableBlocked = s.system || isLastActive;
                    return (
                        <tr key={s.id}>
                            <td style={{ color: "var(--adm-txt-muted)", fontSize: 12 }}>{s.id}</td>
                            <td>
                                <span style={{ fontWeight: 600 }}>{s.name}</span>
                                {s.system && (
                                    <span style={{ marginInlineStart: 6, fontSize: 11, color: "var(--adm-txt-muted)" }}>🔒</span>
                                )}
                            </td>
                            <td>
                                <span className={`adm-badge adm-badge--${s.active ? "active-topic" : "draft"}`}>
                                    {s.active ? t.statusActive : t.statusDraft}
                                </span>
                            </td>
                            <td style={{ color: "var(--adm-txt-2)" }}>{s.subSubjectCount}</td>
                            <td>
                                <span style={{ display: "inline-flex", gap: 5, flexWrap: "wrap" }}>
                                    <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onManageSubSubjects(s)}>
                                        📂 {t.manageSubSubjects}
                                    </button>
                                    <button
                                        className="adm-btn adm-btn--ghost adm-btn--sm"
                                        onClick={() => onEdit(s)}
                                        disabled={s.system}
                                        title={s.system ? t.protectedSubjectNote : ""}
                                    >
                                        ✏️ {t.edit}
                                    </button>
                                    {s.active ? (
                                        <button
                                            className="adm-btn adm-btn--warning adm-btn--sm"
                                            onClick={() => onToggle(s)}
                                            disabled={disableBlocked}
                                            title={s.system ? t.protectedSubjectNote : (isLastActive ? t.lastActiveSubject : "")}
                                        >
                                            {t.disable}
                                        </button>
                                    ) : (
                                        <button
                                            className="adm-btn adm-btn--success adm-btn--sm"
                                            onClick={() => onToggle(s)}
                                            disabled={s.activeSubSubjectCount === 0}
                                            title={s.activeSubSubjectCount === 0 ? t.cannotPublishSubjectEmpty : ""}
                                        >
                                            ✓ {t.publish}
                                        </button>
                                    )}
                                </span>
                                {s.system && (
                                    <div style={{ marginTop: 4, fontSize: 11, color: "var(--adm-txt-muted)" }}>
                                        🔒 {t.protectedSubjectNote}
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
