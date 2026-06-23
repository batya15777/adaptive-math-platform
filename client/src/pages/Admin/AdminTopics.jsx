import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getContentTopics, createTopic, updateTopic, setTopicActive } from "../../service/adminApi.js";
import { TopicsTable } from "../../components/Admin/TopicsTable.jsx";
import { NameFormModal } from "../../components/Admin/NameFormModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";

export const AdminTopics = () => {
    const { language, dir } = useLanguage();
    const t = getAdminStrings(language);
    const navigate = useNavigate();

    const [topics, setTopics] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [editing, setEditing] = useState(null);

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

    const activeCount = topics.filter((t) => t.active).length;

    return (
        <div dir={dir}>
            <div className="adm-page-header">
                <div>
                    <h1 className="adm-page-title">📚 {t.topicsTitle}</h1>
                    <p className="adm-page-subtitle">{t.topicsHint}</p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexShrink: 0 }}>
                    {!loading && (
                        <span style={{ fontSize: 12, color: "var(--adm-txt-muted)" }}>
                            {activeCount} {t.statusActive} / {topics.length}
                        </span>
                    )}
                    <button className="adm-btn adm-btn--primary" onClick={() => setEditing({})}>
                        + {t.newTopic}
                    </button>
                </div>
            </div>

            {error && <div className="adm-notice adm-notice--error">⚠ {error}</div>}

            <div className="adm-card">
                {loading ? (
                    <div className="adm-loading">⏳ {t.loading}</div>
                ) : topics.length === 0 ? (
                    <div className="adm-empty">
                        <div className="adm-empty-icon">📚</div>
                        {t.noData}
                    </div>
                ) : (
                    <TopicsTable
                        topics={topics}
                        onEdit={setEditing}
                        onToggle={handleToggle}
                        onManageSubjects={(topic) => navigate(`/admin/topics/${topic.id}/subjects`)}
                        t={t}
                    />
                )}
            </div>

            <NameFormModal
                open={editing !== null}
                initialName={editing?.name || ""}
                onSubmit={handleSubmit}
                onClose={() => setEditing(null)}
                dir={dir}
                title={editing?.id ? t.editTopic : t.newTopic}
                placeholder={t.topicNamePlaceholder}
                saveLabel={t.save}
                cancelLabel={t.cancel}
            />
        </div>
    );
};
