import { useState, useEffect } from 'react';
import { useProfile } from '../../contexts/useProfile.js';
import { getTopics, getSubjects, getSubSubjects } from '../../service/trainingApi.js';
import { initialAssessment } from '../../service/progressApi.js';
import { LANG_CODE } from '../../i18n/languages.js';

const CONFIDENCE_OPTIONS = [
    { value: 'EASY',   icon: '🤔', title: 'Beginner',    desc: 'Math feels hard for me' },
    { value: 'MEDIUM', icon: '🙂', title: 'Comfortable', desc: 'I know the basics well' },
    { value: 'HARD',   icon: '😎', title: 'Advanced',    desc: 'I enjoy hard problems' },
];

export const SurveyForm = ({ onComplete }) => {
    const { profileData } = useProfile();
    const language = LANG_CODE[profileData?.language] || 'he';

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
        console.log('[Survey] Fetching topics...');
        getTopics()
            .then(res => {
                const topics = res.data || [];
                console.log('[Survey] Topics received:', topics);
                if (topics.length === 0) {
                    setError('No topics available. Please contact support.');
                    setSubjectsLoading(false);
                    return Promise.resolve(null);
                }
                const first = topics[0];
                console.log('[Survey] Auto-selecting topic:', first.name, '| id:', first.id);
                return getSubjects(String(first.id));
            })
            .then(res => {
                if (!res) return;
                const subs = res.data || [];
                console.log('[Survey] Subjects received:', subs);
                setSubjects(subs);
                if (subs.length === 0) {
                    setError('No subjects found for this topic. Please contact support.');
                }
            })
            .catch(err => {
                console.error('[Survey] Error loading subjects:', err?.response?.data || err.message || err);
                setError('Could not load subjects. Please refresh the page.');
            })
            .finally(() => {
                setSubjectsLoading(false);
            });
    }, []);

    const handleSubjectChange = async (subjectId) => {
        console.log('[Survey] Subject selected, id:', subjectId);
        setSelectedSubjectId(subjectId);
        setSubSubjects([]);
        setSelectedSubSubjectIds([]);
        setError('');
        if (!subjectId) return;

        setSubSubjectsLoading(true);
        try {
            const res = await getSubSubjects(subjectId);
            const subs = res.data || [];
            console.log('[Survey] Sub-subjects received:', subs);
            setSubSubjects(subs);
            if (subs.length === 0) {
                setError('No sub-subjects found for this subject.');
            }
        } catch (err) {
            console.error('[Survey] Error loading sub-subjects:', err?.response?.data || err.message || err);
            setError('Could not load sub-subjects. Please try again.');
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
        console.log('[Survey] Submitting — grade:', grade, '| confidence:', confidence,
            '| selectedSubSubjectIds:', selectedSubSubjectIds, '| language:', language);
        setSubmitLoading(true);
        setError('');
        try {
            const res = await initialAssessment({
                subSubjectId:    Number(firstId),
                grade:           Number(grade),
                confidenceLevel: confidence,
                language,
            });
            console.log('[Survey] Assessment question received:', res.data);
            const selectedSS = subSubjects.find(ss => String(ss.id) === firstId);
            const ssName = selectedSS?.name?.toUpperCase() || 'GENERAL';
            onComplete(Number(firstId), res.data, ssName);
        } catch (err) {
            console.error('[Survey] Error starting assessment:', err?.response?.data || err.message || err);
            setError('Could not start the assessment. Please try again.');
        } finally {
            setSubmitLoading(false);
        }
    };

    return (
        <div style={formWrap}>
            <h2 style={sectionHeading}>Tell us about yourself</h2>
            <p style={subtext}>We'll use this to place you at exactly the right level.</p>

            {error && <p style={errorMsg}>{error}</p>}

            {/* Grade */}
            <div style={fieldWrap}>
                <label style={labelStyle}>What grade are you in?</label>
                <select value={grade} onChange={e => setGrade(e.target.value)} style={selectStyle}>
                    <option value="" disabled>— Select grade —</option>
                    {Array.from({ length: 6 }, (_, i) => i + 1).map(g => (
                        <option key={g} value={g}>Grade {g}</option>
                    ))}
                </select>
            </div>

            {/* Confidence */}
            <div style={fieldWrap}>
                <label style={labelStyle}>How confident are you in math?</label>
                <div style={cardRow}>
                    {CONFIDENCE_OPTIONS.map(({ value, icon, title, desc }) => (
                        <button
                            key={value}
                            type="button"
                            onClick={() => setConfidence(value)}
                            style={confidenceCard(confidence === value)}
                        >
                            <span style={{ fontSize: '26px' }}>{icon}</span>
                            <strong style={{ display: 'block', marginTop: '6px', fontSize: '13px' }}>{title}</strong>
                            <span style={{ fontSize: '11px', color: '#777', display: 'block', marginTop: '3px' }}>{desc}</span>
                        </button>
                    ))}
                </div>
            </div>

            {/* Subject */}
            <div style={fieldWrap}>
                <label style={labelStyle}>Subject:</label>
                {subjectsLoading ? (
                    <p style={hint}>Loading subjects...</p>
                ) : (
                    <select
                        value={selectedSubjectId}
                        onChange={e => handleSubjectChange(e.target.value)}
                        style={selectStyle}
                    >
                        <option value="" disabled>— Select subject —</option>
                        {subjects.map(s => (
                            <option key={s.id} value={String(s.id)}>{s.name}</option>
                        ))}
                    </select>
                )}
            </div>

            {/* Sub-subjects — checkbox cards (multi-select) */}
            {selectedSubjectId && (
                <div style={fieldWrap}>
                    <label style={labelStyle}>
                        Choose topics to practice:
                        {selectedSubSubjectIds.length > 0 && (
                            <span style={selectedCount}> {selectedSubSubjectIds.length} selected</span>
                        )}
                    </label>
                    {subSubjectsLoading ? (
                        <p style={hint}>Loading topics...</p>
                    ) : subSubjects.length === 0 ? (
                        <p style={hint}>No topics found for this subject.</p>
                    ) : (
                        <div style={checkboxGrid}>
                            {subSubjects.map(ss => {
                                const strId  = String(ss.id);
                                const active = selectedSubSubjectIds.includes(strId);
                                return (
                                    <div
                                        key={ss.id}
                                        onClick={() => toggleSubSubject(ss.id)}
                                        style={checkboxCard(active)}
                                    >
                                        <span style={checkMark(active)}>✓</span>
                                        <span style={cardLabel}>{ss.name}</span>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}

            <button
                type="button"
                onClick={handleSubmit}
                disabled={!canSubmit}
                style={submitBtn(canSubmit)}
            >
                {submitLoading ? 'Loading...' : 'Start Assessment →'}
            </button>
        </div>
    );
};

// ── styles ──────────────────────────────────────────────────────────────────

const formWrap       = { display: 'flex', flexDirection: 'column', gap: '22px' };
const sectionHeading = { margin: 0, fontSize: '20px', color: '#222' };
const subtext        = { margin: 0, color: '#777', fontSize: '13px' };
const fieldWrap      = { display: 'flex', flexDirection: 'column', gap: '8px' };
const labelStyle     = { fontWeight: '600', fontSize: '14px', color: '#444' };
const hint           = { margin: 0, color: '#999', fontSize: '13px' };
const errorMsg       = { margin: 0, color: '#dc3545', fontSize: '14px' };
const selectedCount  = { fontWeight: 'normal', color: '#007bff', marginLeft: '4px' };

const selectStyle = {
    padding: '10px 12px', fontSize: '15px',
    border: '1px solid #ccc', borderRadius: '6px',
    backgroundColor: '#fff', color: '#333',
};

const cardRow = { display: 'flex', gap: '10px' };

const confidenceCard = (active) => ({
    flex: 1, padding: '12px 8px', textAlign: 'center',
    border: `2px solid ${active ? '#007bff' : '#ddd'}`,
    borderRadius: '8px',
    backgroundColor: active ? '#e8f4ff' : '#fff',
    cursor: 'pointer',
    transition: 'border-color 0.15s, background-color 0.15s',
});

const checkboxGrid = {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '10px',
};

const checkboxCard = (active) => ({
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px',
    padding: '14px 18px',
    minWidth: '80px',
    border: `2px solid ${active ? '#007bff' : '#ddd'}`,
    borderRadius: '8px',
    backgroundColor: active ? '#e8f4ff' : '#fafafa',
    cursor: 'pointer',
    userSelect: 'none',
    transition: 'border-color 0.15s, background-color 0.15s',
});

const checkMark = (active) => ({
    fontSize: '18px',
    color: active ? '#007bff' : '#ddd',
    fontWeight: 'bold',
    transition: 'color 0.15s',
});

const cardLabel = {
    fontSize: '13px',
    fontWeight: '600',
    color: '#333',
    textTransform: 'capitalize',
    textAlign: 'center',
};

const submitBtn = (enabled) => ({
    alignSelf: 'flex-start',
    padding: '12px 28px',
    backgroundColor: enabled ? '#007bff' : '#adb5bd',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    fontSize: '15px',
    fontWeight: 'bold',
    cursor: enabled ? 'pointer' : 'not-allowed',
});
