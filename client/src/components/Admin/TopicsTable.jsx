// Presentational topics table. No delete — topics are published/disabled, not removed.
export const TopicsTable = ({ topics, onEdit, onToggle, onManageSubjects, t }) => (
    <div className="adm-table-wrap">
        <table className="adm-table">
            <thead>
                <tr>
                    {[t.colId, t.colName, t.colStatus, t.colSubjects, t.colActions].map((h, i) => (
                        <th key={i}>{h}</th>
                    ))}
                </tr>
            </thead>
            <tbody>
                {topics.map((topic) => (
                    <tr key={topic.id}>
                        <td style={{ color: "var(--adm-txt-muted)", fontSize: 12 }}>{topic.id}</td>
                        <td style={{ fontWeight: 600 }}>{topic.name}</td>
                        <td>
                            <span className={`adm-badge adm-badge--${topic.active ? "active-topic" : "draft"}`}>
                                {topic.active ? t.statusActive : t.statusDraft}
                            </span>
                        </td>
                        <td style={{ color: "var(--adm-txt-2)" }}>{topic.subjectCount}</td>
                        <td>
                            <span style={{ display: "inline-flex", gap: 6, flexWrap: "wrap" }}>
                                <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onManageSubjects(topic)}>
                                    📚 {t.manageSubjects}
                                </button>
                                <button className="adm-btn adm-btn--ghost adm-btn--sm" onClick={() => onEdit(topic)}>
                                    ✏️ {t.edit}
                                </button>
                                {topic.active ? (
                                    <button className="adm-btn adm-btn--warning adm-btn--sm" onClick={() => onToggle(topic)}>
                                        {t.disable}
                                    </button>
                                ) : (
                                    <button
                                        className="adm-btn adm-btn--success adm-btn--sm"
                                        onClick={() => onToggle(topic)}
                                        disabled={topic.activeSubjectCount === 0}
                                        title={topic.activeSubjectCount === 0 ? t.cannotPublishEmpty : ""}
                                    >
                                        ✓ {t.publish}
                                    </button>
                                )}
                            </span>
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    </div>
);
