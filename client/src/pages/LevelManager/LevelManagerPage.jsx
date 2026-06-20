import { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { SurveyForm } from './SurveyForm.jsx';
import { AssessmentQuestion } from './AssessmentQuestion.jsx';

// PHASES: 'survey' → 'question' → 'done'

export const LevelManagerPage = () => {
    const { user }   = useContext(AuthContext);
    const navigate   = useNavigate();

    const [phase,              setPhase]              = useState('survey');
    const [assessmentQuestion, setAssessmentQuestion] = useState(null);
    const [subSubjectId,       setSubSubjectId]       = useState(null);
    const [subSubjectName,     setSubSubjectName]     = useState('');

    // If already done (e.g. direct URL visit), skip to home
    useEffect(() => {
        if (user && localStorage.getItem(`survey_done_${user.id}`)) {
            navigate('/home', { replace: true });
        }
    }, [user, navigate]);

    // When phase reaches 'done', mark complete and go to math training
    useEffect(() => {
        if (phase === 'done' && user) {
            localStorage.setItem(`survey_done_${user.id}`, '1');
            navigate('/math-training', { replace: true });
        }
    }, [phase, user, navigate]);

    const handleSurveyComplete = (ssId, questionData, ssName) => {
        setSubSubjectId(ssId);
        setAssessmentQuestion(questionData);
        setSubSubjectName(ssName);
        setPhase('question');
    };

    return (
        <div style={page}>
            {/* Step progress bar */}
            <div style={stepsRow}>
                <StepDot num={1} label="About You"           active={phase === 'survey'}   done={phase !== 'survey'} />
                <div style={stepConnector(phase !== 'survey')} />
                <StepDot num={2} label="Assessment Question" active={phase === 'question'} done={phase === 'done'} />
            </div>

            <div style={card}>
                {/* Header */}
                <div style={cardHeader}>
                    <span style={headerEmoji}>📚</span>
                    <div>
                        <h1 style={{ margin: 0, fontSize: '22px', color: '#222' }}>Level Assessment</h1>
                        <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#777' }}>
                            We'll place you at the right level so every question feels just right.
                        </p>
                    </div>
                </div>

                {phase === 'survey' && (
                    <SurveyForm onComplete={handleSurveyComplete} />
                )}

                {phase === 'question' && assessmentQuestion && (
                    <AssessmentQuestion
                        question={assessmentQuestion}
                        subSubjectId={subSubjectId}
                        subSubjectName={subSubjectName}
                        onDone={() => setPhase('done')}
                    />
                )}
            </div>
        </div>
    );
};

// ── Step indicator sub-component ────────────────────────────────────────────

const StepDot = ({ num, label, active, done }) => (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
        <div style={dotStyle(active, done)}>
            {done ? '✓' : num}
        </div>
        <span style={dotLabel(active, done)}>{label}</span>
    </div>
);

// ── styles ──────────────────────────────────────────────────────────────────

const page = {
    padding: '32px 16px',
    maxWidth: '560px',
    margin: '0 auto',
    fontFamily: 'Arial, sans-serif',
};

const stepsRow = {
    display: 'flex',
    alignItems: 'center',
    marginBottom: '28px',
};

const stepConnector = (filled) => ({
    flex: 1,
    height: '2px',
    backgroundColor: filled ? '#007bff' : '#e0e0e0',
    transition: 'background-color 0.3s',
});

const dotStyle = (active, done) => ({
    width: '34px', height: '34px', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontWeight: 'bold', fontSize: '14px',
    backgroundColor: done ? '#28a745' : active ? '#007bff' : '#e9ecef',
    color: done || active ? '#fff' : '#aaa',
    flexShrink: 0,
    transition: 'background-color 0.3s',
});

const dotLabel = (active, done) => ({
    fontSize: '11px', fontWeight: active ? '700' : '400',
    color: done ? '#28a745' : active ? '#007bff' : '#aaa',
    textAlign: 'center', maxWidth: '90px',
    transition: 'color 0.3s',
});

const card = {
    border: '1px solid #ddd',
    borderRadius: '12px',
    padding: '32px',
    backgroundColor: '#fff',
    boxShadow: '0 2px 10px rgba(0,0,0,0.08)',
};

const cardHeader = {
    display: 'flex', alignItems: 'flex-start', gap: '14px',
    marginBottom: '28px', paddingBottom: '20px',
    borderBottom: '1px solid #eee',
};

const headerEmoji = { fontSize: '38px', lineHeight: 1, flexShrink: 0 };
