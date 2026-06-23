import { useState, useEffect, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { getSubjectSubSubjects, createSubSubject, updateSubSubject, setSubSubjectActive } from "../../service/adminApi.js";
import { SubSubjectsTable } from "../../components/Admin/SubSubjectsTable.jsx";
import { NameFormModal } from "../../components/Admin/NameFormModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

// Parent subject/topic info comes from the backend response → correct title and
// a correct "back" link even on a direct visit / refresh.
export const AdminSubSubjects = () => {
    const { subjectId } = useParams();
    const { language, dir } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [editing, setEditing] = useState(null);

    const load = useCallback(() => {
        return getSubjectSubSubjects(subjectId)
            .then((res) => { setData(res.data); setError(""); })
            .catch(() => setError(t.subSubjectsLoadError))
            .finally(() => setLoading(false));
    }, [subjectId, t.subSubjectsLoadError]);

    useEffect(() => { load(); }, [load]);

    const showError = (e) => {
        const backendMsg = e.response?.data?.message;
        if (e.response?.status === 400) setError(t.errSubSubjectRequired);
        else setError(backendMsg || t.errGeneric);
    };

    const handleSubmit = (name) => {
        const req = editing?.id ? updateSubSubject(editing.id, name) : createSubSubject(subjectId, name);
        req
            .then(() => { setEditing(null); return load(); })
            .catch(showError);
    };

    const handleToggle = (ss) => {
        setSubSubjectActive(ss.id, !ss.active)
            .then(load)
            .catch(showError);
    };

    const title = data ? format(t.subSubjectsOf, { subject: data.subjectName }) : t.subSubjectsTitle || "Sub-subjects";
    const activeCount = data?.subSubjects?.filter((ss) => ss.active).length ?? 0;
    const backTo = data ? `/admin/topics/${data.topicId}/subjects` : "/admin/topics";

    return (
        <div dir={dir}>
            <div className="adm-page-header">
                <div>
                    <Link to={backTo} className="adm-back-link" style={{ display: "inline-flex", marginBottom: 8 }}>
                        ← {t.backToSubjects}
                    </Link>
                    <h1 className="adm-page-title">📝 {title}</h1>
                    {data && !data.subjectActive && (
                        <span className="adm-badge adm-badge--draft" style={{ display: "inline-flex", marginTop: 4 }}>{t.statusDraft}</span>
                    )}
                    <p className="adm-page-subtitle" style={{ marginTop: 6 }}>{t.subSubjectsHint}</p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexShrink: 0 }}>
                    {!loading && data && (
                        <span style={{ fontSize: 12, color: "var(--adm-txt-muted)" }}>
                            {activeCount} {t.statusActive} / {data.subSubjects.length}
                        </span>
                    )}
                    {data?.subjectSystem ? (
                        <span style={{ fontSize: 13, color: "var(--adm-txt-muted)" }}>
                            🔒 {t.protectedSubjectNoAdd}
                        </span>
                    ) : (
                        <button className="adm-btn adm-btn--primary" onClick={() => setEditing({})}>
                            + {t.newSubSubject}
                        </button>
                    )}
                </div>
            </div>

            {error && <div className="adm-notice adm-notice--error">⚠ {error}</div>}

            <div className="adm-card">
                {loading ? (
                    <div className="adm-loading">⏳ {t.loading}</div>
                ) : data?.subSubjects?.length === 0 ? (
                    <div className="adm-empty">
                        <div className="adm-empty-icon">📝</div>
                        {t.noData}
                    </div>
                ) : data ? (
                    <SubSubjectsTable
                        subSubjects={data.subSubjects}
                        subjectActive={data.subjectActive}
                        activeSubSubjectCount={data.activeSubSubjectCount}
                        onEdit={setEditing}
                        onToggle={handleToggle}
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
                title={editing?.id ? t.editSubSubject : t.newSubSubject}
                placeholder={t.subSubjectNamePlaceholder}
                saveLabel={t.save}
                cancelLabel={t.cancel}
            />
        </div>
    );
};
