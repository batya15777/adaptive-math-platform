import { Fragment, useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getTopics, getSubjects, getSubSubjects } from '../../service/trainingApi.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { useProfile } from '../../contexts/useProfile.js';
import { format } from '../../i18n/languages.js';
import { getMathTrainingStrings } from './mathTrainingStrings.js';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import '../../styles/spaceTokens.css';
import './MathTraining.css';

// Backend sends subject/topic/practice names in English. Translate the known ones, with a
// graceful fallback to the backend value — so adding more subjects later just works.
const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);
const nameKey = (raw) => {
    const n = (raw || '').toLowerCase();
    if (n.includes('math')) return 'math';
    if (n.includes('quadratic')) return 'quadraticEquations';
    if (n.includes('linear')) return 'linearEquations';
    if (n.includes('road')) return 'roads';
    if (n.includes('percent')) return 'percentages';
    if (n.includes('calc')) return 'calculation';
    if (n.includes('poly')) return 'polynomial';
    if (n.includes('verbal') || n.includes('word')) return 'verbal';
    if (n.includes('add')) return 'add';
    if (n.includes('sub')) return 'sub';
    if (n.includes('mult')) return 'mult';
    if (n.includes('div')) return 'div';
    if (n.includes('frac')) return 'fractions';
    if (n.includes('dec')) return 'decimals';
    return null;
};
const trName = (raw, t) => { const k = nameKey(raw); return (k && t.names?.[k]) || cap(raw); };
const GLYPH = {
    add: '+', sub: '−', mult: '×', div: '÷', fractions: '½', decimals: '.5',
    calculation: '🧮', polynomial: 'x²', verbal: '💬', math: '➗',
    roads: '🛣️', percentages: '%', linearEquations: '📈', quadraticEquations: 'x²',
};
const glyphFor = (raw) => GLYPH[nameKey(raw)] || '★';

// A small purple orbital "planet" with the item's glyph (matches the sketch).
const Orb = ({ raw, variant = '' }) => (
    <span className={`mt-orb ${variant}`} aria-hidden="true">
        <span className="mt-orb-planet">{glyphFor(raw)}</span>
    </span>
);

// Step pill: Subjects ① ‹ Topics ② ‹ Levels ③ — current highlighted, earlier ones ticked.
const StepIndicator = ({ step, t }) => {
    const labels = [t.stepSubjects, t.stepTopics, t.stepPractices];
    return (
        <div className="mt-steps">
            {labels.map((label, i) => {
                const n = i + 1;
                const state = n === step ? 'is-active' : n < step ? 'is-done' : '';
                return (
                    <Fragment key={n}>
                        {i > 0 && <span className="mt-step-sep">‹</span>}
                        <span className={`mt-step ${state}`}>
                            <span className="mt-step-num">{n < step ? '✓' : n}</span>
                            {label}
                        </span>
                    </Fragment>
                );
            })}
        </div>
    );
};

// Drill-down: subject (topic) → topic (subject) → practice (sub-subject, with progress).
// Clicking a practice opens the regular exercise (QuestionGame) for it.
export const MathTraining = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { language, dir } = useLanguage();
    const { profileData } = useProfile();
    const theme = (profileData.theme || 'LIGHT').toLowerCase();
    const t = getMathTrainingStrings(language);

    const [topics,      setTopics]      = useState([]);
    const [subjects,    setSubjects]    = useState([]);
    const [subSubjects, setSubSubjects] = useState([]);

    const [selectedTopic, setSelectedTopic] = useState(() => location.state?.topic ?? null);
    const [selectedSubject, setSelectedSubject] = useState(() => location.state?.subject ?? null);

    const [loading, setLoading] = useState(false);
    const [error,   setError]   = useState('');

    // Small helper so every fetch shares the same loading/error handling.
    const load = useCallback(async (fn, apply) => {
        setLoading(true);
        setError('');
        try {
            const res = await fn();
            apply(res.data);
        } catch {
            setError(t.errGeneric);
        } finally {
            setLoading(false);
        }
        // `t.errGeneric` is intentionally omitted: `load` should stay stable so
        // changing language doesn't re-trigger the fetch effects below.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // On mount: load topics. If we arrived back from the game (state carries the topic +
    // subject), restore the sub-subjects view so "back" lands on that subject's tab.
    useEffect(() => {
        const topic = location.state?.topic;
        const subject = location.state?.subject;
        if (topic && subject) {
            getTopics().then(r => setTopics(r.data)).catch(() => {});
            getSubjects(topic.id).then(r => setSubjects(r.data)).catch(() => {});
            // eslint-disable-next-line react-hooks/set-state-in-effect
            load(() => getSubSubjects(subject.id), setSubSubjects);
        } else {
            load(getTopics, setTopics);
        }
    }, [load, location.state]);

    // Step 2 — pick a subject (topic) → its topics (subjects)
    const handleTopicClick = (topic) => {
        setSelectedTopic(topic);
        setSelectedSubject(null);
        setSubSubjects([]);
        load(() => getSubjects(topic.id), setSubjects);
    };

    // Step 3 — pick a topic (subject) → its practices (sub-subjects, with progress)
    const handleSubjectClick = (subject) => {
        setSelectedSubject(subject);
        load(() => getSubSubjects(subject.id), setSubSubjects);
    };

    // Breadcrumb navigation — step back up a level.
    const goToTopics = () => {
        setSelectedTopic(null);
        setSelectedSubject(null);
        setSubjects([]);
        setSubSubjects([]);
        setError('');
    };
    const goToSubjects = () => {
        setSelectedSubject(null);
        setSubSubjects([]);
        setError('');
    };

    const step = selectedSubject ? 3 : selectedTopic ? 2 : 1;
    const subtitle = step === 1 ? t.subSubjects : step === 2 ? t.subTopics : t.subPractices;

    return (
        <div className="mg-space mt-root" data-theme={theme} dir={dir}>
            <Stars />
            <div className="sc-content mt-topbar"><AppTopBar /></div>

            <div className="sc-content mt-inner">
                <header className="mt-head">
                    <h1 className="mt-title">{t.title}</h1>
                    <p className="mt-sub">{subtitle}</p>
                </header>

                <StepIndicator step={step} t={t} />

                <div className="mt-crumbs">
                    <button type="button" className={`mt-crumb${selectedTopic ? ' is-link' : ' is-current'}`} onClick={selectedTopic ? goToTopics : undefined}>
                        {t.stepSubjects}
                    </button>
                    {selectedTopic && (
                        <>
                            <span className="mt-crumb-sep">›</span>
                            <button type="button" className={`mt-crumb${selectedSubject ? ' is-link' : ' is-current'}`} onClick={selectedSubject ? goToSubjects : undefined}>
                                {trName(selectedTopic.name, t)}
                            </button>
                        </>
                    )}
                    {selectedSubject && (
                        <>
                            <span className="mt-crumb-sep">›</span>
                            <span className="mt-crumb is-current">{trName(selectedSubject.name, t)}</span>
                        </>
                    )}
                </div>

                {loading && <p className="mt-msg">{t.loading}</p>}
                {error && <p className="mt-msg mt-msg--err">{error}</p>}

                {/* Step 1: subjects (disciplines) */}
                {!selectedTopic && !loading && (
                    <div className={`mt-grid${topics.length === 1 ? ' mt-grid--one' : ''}`}>
                        {topics.map(topic => (
                            <button key={topic.id} type="button" className="mt-card mt-card--lg" onClick={() => handleTopicClick(topic)}>
                                <Orb raw={topic.name} variant="mt-orb--lg" />
                                <span className="mt-card-body">
                                    <div className="mt-card-title">{trName(topic.name, t)}</div>
                                    <div className="mt-card-hint">{t.enterHint}</div>
                                </span>
                                <span className="mt-card-enter" aria-hidden="true">‹</span>
                            </button>
                        ))}
                        {topics.length === 0 && !error && <p className="mt-msg">{t.emptyTopics}</p>}
                    </div>
                )}

                {/* Step 2: topics (areas) */}
                {selectedTopic && !selectedSubject && !loading && (
                    <div className="mt-grid">
                        {subjects.map(subject => (
                            <button key={subject.id} type="button" className="mt-card mt-card--topic" onClick={() => handleSubjectClick(subject)}>
                                <div className="mt-card-title">{trName(subject.name, t)}</div>
                                <Orb raw={subject.name} variant="mt-orb--lg mt-orb--topic" />
                            </button>
                        ))}
                        {subjects.length === 0 && !error && <p className="mt-msg">{t.emptySubjects}</p>}
                    </div>
                )}

                {/* Step 3: practices (sub-subjects) with progress */}
                {selectedSubject && !loading && (
                    <div className="mt-grid">
                        {subSubjects.map(sub => (
                            <button
                                key={sub.id}
                                type="button"
                                className="mt-card mt-card--practice"
                                onClick={() => navigate(`/math-training/${sub.id}/play`, { state: { subSubjectName: sub.name, topic: selectedTopic, subject: selectedSubject } })}
                            >
                                <div className="mt-practice-head">
                                    <span className="mt-card-title">{trName(sub.name, t)}</span>
                                    <Orb raw={sub.name} />
                                </div>
                                {/* Three independent metrics — do NOT merge:
                                    Level      = currentLevel — progression tier.
                                    Solved     = questionsSolved — correct answers in this practice.
                                    Difficulty = difficultyLevel — the ML-adjusted difficulty actually played. */}
                                <div className="mt-stats">
                                    <div className="mt-stat"><div className="mt-stat-v">{sub.currentLevel}</div><div className="mt-stat-l">{t.statLevel}</div></div>
                                    <div className="mt-stat"><div className="mt-stat-v">{sub.questionsSolved}</div><div className="mt-stat-l">{t.statSolved}</div></div>
                                    <div className="mt-stat"><div className="mt-stat-v">{sub.difficultyLevel}</div><div className="mt-stat-l">{t.statDifficulty}</div></div>
                                </div>
                                <span className="mt-start">▶ {t.start}</span>
                            </button>
                        ))}
                        {subSubjects.length === 0 && !error && <p className="mt-msg">{t.emptySubSubjects}</p>}
                    </div>
                )}

                <div className="mt-badge">🚀 {format(t.stepOf, { n: step, total: 3 })}</div>
            </div>
        </div>
    );
};
