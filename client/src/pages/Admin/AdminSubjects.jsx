import { useState, useEffect, useCallback } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { getTopicSubjects, createSubject, updateSubject, setSubjectActive } from "../../service/adminApi.js";
import { SubjectsTable } from "../../components/Admin/SubjectsTable.jsx";
import { NameFormModal } from "../../components/Admin/NameFormModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

// The topic info (name/active) comes from the backend response, so the title is
// correct even on a direct visit / refresh.
export const AdminSubjects = () => {
    const { topicId } = useParams();
    const { language, dir } = useLanguage();
    const t = getAdminStrings(language);
    const navigate = useNavigate();

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [editing, setEditing] = useState(null);

    const load = useCallback(() => {
        return getTopicSubjects(topicId)
            .then((res) => { setData(res.data); setError(""); })
            .catch(() => setError(t.subjectsLoadError))
            .finally(() => setLoading(false));
    }, [topicId, t.subjectsLoadError]);

    useEffect(() => { load(); }, [load]);

    const handleSubmit = (name) => {
        const req = editing?.id ? updateSubject(editing.id, name) : createSubject(topicId, name);
        req
            .then(() => { setEditing(null); return load(); })
            .catch((e) => {
                const status = e.response?.status;
                if (status === 409) setError(t.errSubjectDuplicate);
                else if (status === 400) setError(t.errSubjectRequired);
                else setError(t.errGeneric);
            });
    };

    const handleToggle = (subject) => {
        setSubjectActive(subject.id, !subject.active)
            .then(load)
            .catch(() => setError(t.errGeneric));
    };

    const title = data ? format(t.subjectsOf, { topic: data.topicName }) : t.subjectsTitle;
    const activeCount = data?.subjects?.filter((s) => s.active).length ?? 0;

    return (
        <div dir={dir}>
            <div className="adm-page-header">
                <div>
                    <Link to="/admin/topics" className="adm-back-link" style={{ display: "inline-flex", marginBottom: 8 }}>
                        ← {t.backToTopics}
                    </Link>
                    <h1 className="adm-page-title">📖 {title}</h1>
                    {data && !data.topicActive && (
                        <span className="adm-badge adm-badge--draft" style={{ display: "inline-flex", marginTop: 4 }}>{t.statusDraft}</span>
                    )}
                    <p className="adm-page-subtitle" style={{ marginTop: 6 }}>{t.subjectsHint}</p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexShrink: 0 }}>
                    {!loading && data && (
                        <span style={{ fontSize: 12, color: "var(--adm-txt-muted)" }}>
                            {activeCount} {t.statusActive} / {data.subjects.length}
                        </span>
                    )}
                    <button className="adm-btn adm-btn--primary" onClick={() => setEditing({})}>
                        + {t.newSubject}
                    </button>
                </div>
            </div>

            {error && <div className="adm-notice adm-notice--error">⚠ {error}</div>}

            <div className="adm-card">
                {loading ? (
                    <div className="adm-loading">⏳ {t.loading}</div>
                ) : data?.subjects?.length === 0 ? (
                    <div className="adm-empty">
                        <div className="adm-empty-icon">📖</div>
                        {t.noData}
                    </div>
                ) : data ? (
                    <SubjectsTable
                        subjects={data.subjects}
                        topicActive={data.topicActive}
                        activeSubjectCount={data.activeSubjectCount}
                        onEdit={setEditing}
                        onToggle={handleToggle}
                        onManageSubSubjects={(s) => navigate(`/admin/subjects/${s.id}/sub-subjects`)}
                        t={t}
                    />
                ) : null}
            </div>

            <NameFormModal
                open={editing !== null}
                initialName={editing?.name || ""}
                onSubmit={handleSubmit}
                onClose={() => setEditing(null)}
                dir={dir}
                title={editing?.id ? t.editSubject : t.newSubject}
                placeholder={t.subjectNamePlaceholder}
                saveLabel={t.save}
                cancelLabel={t.cancel}
            />
        </div>
    );
};
