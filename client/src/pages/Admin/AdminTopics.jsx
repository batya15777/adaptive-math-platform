import { useState, useEffect, useCallback } from "react";
import { getContentTopics, createTopic, updateTopic, setTopicActive } from "../../service/adminApi.js";
import { TopicsTable } from "../../components/Admin/TopicsTable.jsx";
import { TopicFormModal } from "../../components/Admin/TopicFormModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";

// "Smart" page: owns data fetching and modal/loading/error state.
// Presentational pieces (TopicsTable, TopicFormModal) live in components/Admin/.
export const AdminTopics = () => {
    const { language, dir } = useLanguage();
    const t = getAdminStrings(language);

    const [topics, setTopics] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [editing, setEditing] = useState(null); // null = modal closed, {} = create, {id,name} = edit

    const load = useCallback(() => {
        return getContentTopics()
            .then((res) => { setTopics(res.data || []); setError(""); })
            .catch(() => setError(t.topicsLoadError))
            .finally(() => setLoading(false));
    }, [t.topicsLoadError]);

    useEffect(() => { load(); }, [load]);

    const handleSubmit = (name) => {
        const req = editing?.id ? updateTopic(editing.id, name) : createTopic(name);
        req
            .then(() => { setEditing(null); return load(); })
            .catch((e) => {
                const status = e.response?.status;
                if (status === 409) setError(t.errDuplicateName);
                else if (status === 400) setError(t.errNameRequired);
                else setError(t.errGeneric);
            });
    };

    const handleToggle = (topic) => {
        setTopicActive(topic.id, !topic.active)
            .then(load)
            .catch((e) => {
                if (e.response?.status === 409) setError(t.cannotPublishEmpty);
                else setError(t.errGeneric);
            });
    };

    return (
        <div style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <h1>{t.topicsTitle}</h1>
            <p style={{ color: "#888", fontSize: 14 }}>{t.topicsHint}</p>
            <button onClick={() => setEditing({})} style={{ marginBottom: 16 }}>+ {t.newTopic}</button>
            {error && <p style={{ color: "#dc3545" }}>{error}</p>}
            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
            ) : (
                <TopicsTable topics={topics} onEdit={setEditing} onToggle={handleToggle} t={t} />
            )}

            <TopicFormModal
                open={editing !== null}
                initialName={editing?.name || ""}
                onSubmit={handleSubmit}
                onClose={() => setEditing(null)}
                t={t}
                dir={dir}
            />
        </div>
    );
};
