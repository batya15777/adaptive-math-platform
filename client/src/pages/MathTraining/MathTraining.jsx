import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getTopics, getSubjects, getSubSubjects } from '../../service/trainingApi.js';

// Drill-down: topic → subject → sub-subjects (with the student's progress on each).
// Clicking a sub-subject opens the question game for it.
export const MathTraining = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [topics,      setTopics]      = useState([]);
    const [subjects,    setSubjects]    = useState([]);
    const [subSubjects, setSubSubjects] = useState([]);

    const [selectedTopic,   setSelectedTopic]   = useState(null);
    const [selectedSubject, setSelectedSubject] = useState(null);

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
            setError('Something went wrong. Please try again.');
        } finally {
            setLoading(false);
        }
    }, []);

    // On mount: load topics. If we arrived back from the game (state carries the topic +
    // subject), restore the sub-subjects view so "back" lands on that subject's tab.
    useEffect(() => {
        const t = location.state?.topic;
        const s = location.state?.subject;
        if (t && s) {
            setSelectedTopic(t);
            setSelectedSubject(s);
            getTopics().then(r => setTopics(r.data)).catch(() => {});        // for breadcrumb back
            getSubjects(t.id).then(r => setSubjects(r.data)).catch(() => {}); // for breadcrumb back
            load(() => getSubSubjects(s.id), setSubSubjects);                 // the restored view
        } else {
            load(getTopics, setTopics);
        }
    }, [load, location.state]);

    // Step 2 — pick a topic → its subjects
    const handleTopicClick = (topic) => {
        setSelectedTopic(topic);
        setSelectedSubject(null);
        setSubSubjects([]);
        load(() => getSubjects(topic.id), setSubjects);
    };

    // Step 3 — pick a subject → its sub-subjects (with progress)
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

    return (
        <div style={page}>
            <h2 style={{ color: '#333', marginBottom: '8px' }}>Math Training</h2>

            {/* Breadcrumb */}
            <div style={crumbBar}>
                <span style={selectedTopic ? crumbLink : crumbCurrent} onClick={selectedTopic ? goToTopics : undefined}>
                    Topics
                </span>
                {selectedTopic && (
                    <>
                        <span style={crumbSep}>›</span>
                        <span style={selectedSubject ? crumbLink : crumbCurrent} onClick={selectedSubject ? goToSubjects : undefined}>
                            {selectedTopic.name}
                        </span>
                    </>
                )}
                {selectedSubject && (
                    <>
                        <span style={crumbSep}>›</span>
                        <span style={crumbCurrent}>{selectedSubject.name}</span>
                    </>
                )}
            </div>

            {loading && <p style={{ color: '#888' }}>Loading...</p>}
            {error   && <p style={{ color: '#dc3545' }}>{error}</p>}

            {/* Level 1: topics */}
            {!selectedTopic && !loading && (
                <Grid>
                    {topics.map(topic => (
                        <SelectCard key={topic.id} onClick={() => handleTopicClick(topic)}>
                            <span style={cardTitle}>{topic.name}</span>
                        </SelectCard>
                    ))}
                    {topics.length === 0 && !error && <Empty>No topics available yet.</Empty>}
                </Grid>
            )}

            {/* Level 2: subjects */}
            {selectedTopic && !selectedSubject && !loading && (
                <Grid>
                    {subjects.map(subject => (
                        <SelectCard key={subject.id} onClick={() => handleSubjectClick(subject)}>
                            <span style={cardTitle}>{subject.name}</span>
                        </SelectCard>
                    ))}
                    {subjects.length === 0 && !error && <Empty>No subjects under this topic yet.</Empty>}
                </Grid>
            )}

            {/* Level 3: sub-subjects with progress */}
            {selectedSubject && !loading && (
                <Grid>
                    {subSubjects.map(sub => (
                        <div
                            key={sub.id}
                            style={statsCard}
                            onClick={() => navigate(`/math-training/${sub.id}/play`, { state: { subSubjectName: sub.name, topic: selectedTopic, subject: selectedSubject } })}
                            onMouseEnter={e => { e.currentTarget.style.borderColor = '#007bff'; e.currentTarget.style.boxShadow = '0 2px 8px #007bff33'; }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = '#ddd';     e.currentTarget.style.boxShadow = 'none'; }}
                        >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={cardTitle}>{sub.name}</span>
                                <span style={{ color: '#007bff', fontWeight: 'bold', fontSize: '14px' }}>Play ▶</span>
                            </div>
                            <div style={statsRow}>
                                <Stat label="Level"      value={sub.currentLevel} />
                                <Stat label="Solved"     value={sub.questionsSolved} />
                                <Stat label="Difficulty" value={sub.difficultyLevel} />
                            </div>
                        </div>
                    ))}
                    {subSubjects.length === 0 && !error && <Empty>No sub-subjects under this subject yet.</Empty>}
                </Grid>
            )}
        </div>
    );
};

// ── small presentational helpers ──────────────────────────────────────────────

const Grid = ({ children }) => (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', marginTop: '16px' }}>{children}</div>
);

const SelectCard = ({ children, onClick }) => (
    <div
        onClick={onClick}
        style={selectCard}
        onMouseEnter={e => { e.currentTarget.style.borderColor = '#007bff'; e.currentTarget.style.boxShadow = '0 2px 8px #007bff33'; }}
        onMouseLeave={e => { e.currentTarget.style.borderColor = '#ddd';     e.currentTarget.style.boxShadow = 'none'; }}
    >
        {children}
    </div>
);

const Stat = ({ label, value }) => (
    <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#007bff' }}>{value}</div>
        <div style={{ fontSize: '12px', color: '#888' }}>{label}</div>
    </div>
);

const Empty = ({ children }) => (
    <p style={{ color: '#888', fontStyle: 'italic' }}>{children}</p>
);

// ── styles ─────────────────────────────────────────────────────────────────────

const page = { padding: '20px', maxWidth: '720px', margin: '0 auto', fontFamily: 'Arial, sans-serif' };

const crumbBar     = { display: 'flex', alignItems: 'center', gap: '8px', margin: '8px 0 4px', fontSize: '14px' };
const crumbLink    = { color: '#007bff', cursor: 'pointer', fontWeight: 'bold' };
const crumbCurrent = { color: '#333', fontWeight: 'bold' };
const crumbSep     = { color: '#aaa' };

const cardTitle = { fontSize: '16px', fontWeight: 'bold', color: '#333', textTransform: 'capitalize' };

const selectCard = {
    minWidth: '140px', padding: '20px', borderRadius: '8px',
    border: '1px solid #ddd', backgroundColor: '#f9f9f9', cursor: 'pointer',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    transition: 'border-color 0.15s, box-shadow 0.15s',
};

const statsCard = {
    minWidth: '220px', padding: '16px 20px', borderRadius: '8px',
    border: '1px solid #ddd', backgroundColor: '#f0f8ff',
    display: 'flex', flexDirection: 'column', gap: '12px',
    cursor: 'pointer', transition: 'border-color 0.15s, box-shadow 0.15s',
};

const statsRow = { display: 'flex', justifyContent: 'space-around', gap: '12px' };
