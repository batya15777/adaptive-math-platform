import { useState, useEffect, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { getSubjectSubSubjects, createSubSubject, updateSubSubject, setSubSubjectActive } from "../../service/adminApi.js";
import { SubSubjectsTable } from "../../components/Admin/SubSubjectsTable.jsx";
import { NameFormModal } from "../../components/Admin/NameFormModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

// "Smart" page for managing the sub-subjects of one subject.
// Parent subject/topic info comes from the backend response → correct title and a
// correct "back" link even on a direct visit / refresh.
export const AdminSubSubjects = () => {
    const { subjectId } = useParams();
    const { language, dir } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null); // { subjectName, subjectActive, subjectSystem, topicId, activeSubSubjectCount, subSubjects }
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [editing, setEditing] = useState(null); // null = closed, {} = create, {id,name} = edit

    const load = useCallback(() => {
        return getSubjectSubSubjects(subjectId)
            .then((res) => { setData(res.data); setError(""); })
            .catch(() => setError(t.subSubjectsLoadError))
            .finally(() => setLoading(false));
    }, [subjectId, t.subSubjectsLoadError]);

    useEffect(() => { load(); }, [load]);

    // 409 has several causes here (duplicate / no templates / protected / last-active),
    // so never assume "duplicate": show a clear backend message if present, else generic.
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

    const title = data ? format(t.subSubjectsOf, { subject: data.subjectName }) : t.subSubjectsTitle;

    return (
        <div dir={dir} style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <p style={{ marginTop: 0 }}>
                <Link to={data ? `/admin/topics/${data.topicId}/subjects` : "/admin/topics"}>{t.backToSubjects}</Link>
            </p>
            <h1>{title}</h1>
            {data && !data.subjectActive && (
                <p style={{ color: "#6c757d", fontSize: 13 }}>{t.statusDraft}</p>
            )}
            <p style={{ color: "#888", fontSize: 14 }}>{t.subSubjectsHint}</p>

            {/* Cannot add sub-subjects to a protected system subject (e.g. Calculation). */}
            {data?.subjectSystem ? (
                <p style={{ color: "#6c757d", fontSize: 14 }}>🔒 {t.protectedSubjectNoAdd}</p>
            ) : (
                <button onClick={() => setEditing({})} style={{ marginBottom: 16 }}>+ {t.newSubSubject}</button>
            )}

            {error && <p style={{ color: "#dc3545" }}>{error}</p>}
            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
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
