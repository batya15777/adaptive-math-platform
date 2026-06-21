import { useState, useEffect, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { getTopicSubjects, createSubject, updateSubject, setSubjectActive } from "../../service/adminApi.js";
import { SubjectsTable } from "../../components/Admin/SubjectsTable.jsx";
import { NameFormModal } from "../../components/Admin/NameFormModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

// "Smart" page for managing the subjects of one topic.
// The topic info (name/active) comes from the backend response, so the title is
// correct even on a direct visit / refresh of /admin/topics/:topicId/subjects.
export const AdminSubjects = () => {
    const { topicId } = useParams();
    const { language, dir } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null); // { topicName, topicActive, activeSubjectCount, subjects }
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [editing, setEditing] = useState(null); // null = closed, {} = create, {id,name} = edit

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
            // The UI already disables the blocked cases; any 409 here falls back to generic.
            .catch(() => setError(t.errGeneric));
    };

    const title = data ? format(t.subjectsOf, { topic: data.topicName }) : t.subjectsTitle;

    return (
        <div dir={dir} style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <p style={{ marginTop: 0 }}><Link to="/admin/topics">{t.backToTopics}</Link></p>
            <h1>{title}</h1>
            {data && !data.topicActive && (
                <p style={{ color: "#6c757d", fontSize: 13 }}>{t.statusDraft}</p>
            )}
            <p style={{ color: "#888", fontSize: 14 }}>{t.subjectsHint}</p>
            <button onClick={() => setEditing({})} style={{ marginBottom: 16 }}>+ {t.newSubject}</button>
            {error && <p style={{ color: "#dc3545" }}>{error}</p>}
            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
            ) : data ? (
                <SubjectsTable
                    subjects={data.subjects}
                    topicActive={data.topicActive}
                    activeSubjectCount={data.activeSubjectCount}
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
                title={editing?.id ? t.editSubject : t.newSubject}
                placeholder={t.subjectNamePlaceholder}
                saveLabel={t.save}
                cancelLabel={t.cancel}
            />
        </div>
    );
};
