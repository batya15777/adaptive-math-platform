import { useState, useEffect } from 'react';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getTopics, getSubjects, getSubSubjects } from '../../service/trainingApi.js';
import { initialAssessment } from '../../service/progressApi.js';
import { ThemedSelect } from '../../components/ui/ThemedSelect.jsx';

export const SurveyForm = ({ onComplete, t }) => {
    const { language } = useLanguage();

    // Confidence options: stable value/icon, localized title/desc from `t`.
    const confidenceOptions = [
        { value: 'EASY',   icon: '🤔', title: t.confBeginnerTitle,    desc: t.confBeginnerDesc },
        { value: 'MEDIUM', icon: '🙂', title: t.confComfortableTitle, desc: t.confComfortableDesc },
        { value: 'HARD',   icon: '😎', title: t.confAdvancedTitle,    desc: t.confAdvancedDesc },
    ];

    const [grade,      setGrade]      = useState('');
    const [confidence, setConfidence] = useState('');

    const [subjects,    setSubjects]    = useState([]);
    const [subSubjects, setSubSubjects] = useState([]);

    const [selectedSubjectId,     setSelectedSubjectId]     = useState('');
    const [selectedSubSubjectIds, setSelectedSubSubjectIds] = useState([]);

    const [subjectsLoading,    setSubjectsLoading]    = useState(true);
    const [subSubjectsLoading, setSubSubjectsLoading] = useState(false);
    const [submitLoading,      setSubmitLoading]      = useState(false);
    const [error,              setError]              = useState('');

    // On mount: fetch topics → auto-use first (Math) → fetch its subjects
    useEffect(() => {
        getTopics()
            .then(res => {
                const topics = res.data || [];
                if (topics.length === 0) {
                    setError(t.errNoTopics);
                    setSubjectsLoading(false);
                    return Promise.resolve(null);
                }
                const first = topics[0];
                return getSubjects(String(first.id));
            })
            .then(res => {
                if (!res) return;
                const subs = res.data || [];
                setSubjects(subs);
                if (subs.length === 0) {
                    setError(t.errNoSubjects);
                }
            })
            .catch(() => {
                setError(t.errLoadSubjects);
            })
            .finally(() => {
                setSubjectsLoading(false);
            });
        // Mount-only fetch; error strings come from `t` but we don't want to
        // re-fetch topics when the language changes.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleSubjectChange = async (subjectId) => {
        setSelectedSubjectId(subjectId);
        setSubSubjects([]);
        setSelectedSubSubjectIds([]);
        setError('');
        if (!subjectId) return;

        setSubSubjectsLoading(true);
        try {
            const res = await getSubSubjects(subjectId);
            const subs = res.data || [];
            setSubSubjects(subs);
            if (subs.length === 0) {
                setError(t.errNoSubSubjects);
            }
        } catch {
            setError(t.errLoadSubSubjects);
        } finally {
            setSubSubjectsLoading(false);
        }
    };

    const toggleSubSubject = (id) => {
        const strId = String(id);
        setSelectedSubSubjectIds(prev =>
            prev.includes(strId) ? prev.filter(x => x !== strId) : [...prev, strId]
        );
    };

    const canSubmit = grade && confidence && selectedSubSubjectIds.length > 0 && !submitLoading;

    const handleSubmit = async () => {
        if (!canSubmit) return;
        const firstId = selectedSubSubjectIds[0];
        setSubmitLoading(true);
        setError('');
        try {
            const res = await initialAssessment({
                subSubjectId:    Number(firstId),
                grade:           Number(grade),
                confidenceLevel: confidence,
                language,
            });
            const selectedSS = subSubjects.find(ss => String(ss.id) === firstId);
            const ssName = selectedSS?.name?.toUpperCase() || 'GENERAL';
            onComplete(Number(firstId), res.data, ssName);
        } catch {
            setError(t.errStartAssessment);
        } finally {
            setSubmitLoading(false);
        }
    };

    return (
        <div>
            <div className="lvl-head">
                <h2>{t.surveyHeading}</h2>
                <p>{t.surveySubtext}</p>
            </div>
            <div className="lvl-divider" />

            {error && <p className="lvl-error">{error}</p>}

            <div className="lvl-form">
                {/* Grade */}
                <div className="lvl-field">
                    <label className="lvl-label">{t.gradeLabel}</label>
                    <ThemedSelect
                        value={grade}
                        onChange={setGrade}
                        placeholder={t.gradePlaceholder}
                        ariaLabel={t.gradeLabel}
                        options={t.gradeOptions.map((label, i) => ({
                            value: String(i + 1),
                            label,
                        }))}
                    />
                </div>

                {/* Confidence */}
                <div className="lvl-field">
                    <label className="lvl-label">{t.confidenceLabel}</label>
                    <div className="lvl-conf-row">
                        {confidenceOptions.map(({ value, icon, title, desc }) => (
                            <button
                                key={value}
                                type="button"
                                onClick={() => setConfidence(value)}
                                className={'lvl-conf' + (confidence === value ? ' is-sel' : '')}
                            >
                                <span className="lvl-conf-ico">{icon}</span>
                                <strong className="lvl-conf-title">{title}</strong>
                                <span className="lvl-conf-desc">{desc}</span>
                            </button>
                        ))}
                    </div>
                </div>

                {/* Subject */}
                <div className="lvl-field">
                    <label className="lvl-label">{t.subjectLabel}</label>
                    {subjectsLoading ? (
                        <p className="lvl-loading">{t.subjectLoading}</p>
                    ) : (
                        <ThemedSelect
                            value={selectedSubjectId}
                            onChange={handleSubjectChange}
                            placeholder={t.subjectPlaceholder}
                            ariaLabel={t.subjectLabel}
                            options={subjects.map(s => ({ value: String(s.id), label: s.name }))}
                        />
                    )}
                </div>

                {/* Sub-subjects — chips (multi-select) */}
                {selectedSubjectId && (
                    <div className="lvl-field">
                        <label className="lvl-label">
                            {t.chooseTopics}
                            {selectedSubSubjectIds.length > 0 && (
                                <span className="lvl-count">{format(t.selectedCount, { count: selectedSubSubjectIds.length })}</span>
                            )}
                        </label>
                        {subSubjectsLoading ? (
                            <p className="lvl-loading">{t.topicsLoading}</p>
                        ) : subSubjects.length === 0 ? (
                            <p className="lvl-loading">{t.noTopicsForSubject}</p>
                        ) : (
                            <div className="lvl-topics">
                                {subSubjects.map(ss => {
                                    const strId  = String(ss.id);
                                    const active = selectedSubSubjectIds.includes(strId);
                                    return (
                                        <div
                                            key={ss.id}
                                            onClick={() => toggleSubSubject(ss.id)}
                                            className={'lvl-topic' + (active ? ' is-sel' : '')}
                                        >
                                            <span className="lvl-topic-check">✓</span>
                                            <span>{ss.name}</span>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}

                <button type="button" className="mg-cta" onClick={handleSubmit} disabled={!canSubmit}>
                    {submitLoading ? t.loading : t.startAssessment}
                </button>
            </div>
        </div>
    );
};
