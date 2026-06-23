import { useState, useEffect, useRef, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { getLevelManagerStrings } from './levelManagerStrings.js';
import { AuthLayout } from '../../components/auth/AuthLayout.jsx';
import { SurveyForm } from './SurveyForm.jsx';
import { AssessmentQuestion } from './AssessmentQuestion.jsx';
import { markSurveyComplete } from '../../service/authApi.js';
import './levelSurvey.css';

// PHASES: 'survey' → 'question' → 'done'

export const LevelManagerPage = () => {
    const { user, completeSurvey } = useContext(AuthContext);
    const navigate   = useNavigate();
    const { language } = useLanguage();
    const t = getLevelManagerStrings(language);

    const [phase,              setPhase]              = useState('survey');
    const [assessmentQuestion, setAssessmentQuestion] = useState(null);
    const [subSubjectId,       setSubSubjectId]       = useState(null);
    const [subSubjectName,     setSubSubjectName]     = useState('');

    // Ref prevents the done-phase effect from firing more than once.
    // This is critical: completeSurvey() changes the `user` object in AuthContext,
    // which would re-trigger the effect (user is a dep) without this guard,
    // causing an infinite markSurveyComplete() → completeSurvey() → re-render loop.
    const completionCalledRef = useRef(false);

    // Redirect users who are already done (direct URL visit or page refresh while done).
    // The `phase !== 'done'` guard prevents this from firing mid-completion and
    // conflicting with the navigation below.
    useEffect(() => {
        if (phase !== 'done' && user?.hasCompletedSurvey) {
            navigate('/home', { replace: true });
        }
    }, [phase, user, navigate]);

    // When the last assessment question is answered, persist to the server and lift the gate.
    // .finally() ensures navigate always runs (success or network failure).
    useEffect(() => {
        if (phase !== 'done' || !user || completionCalledRef.current) return;
        completionCalledRef.current = true;

        markSurveyComplete()
            .finally(() => {
                completeSurvey();                           // patch React state — gate lifts on next render
                navigate('/math-training', { replace: true });
            });
    }, [phase, user, completeSurvey, navigate]);

    const handleSurveyComplete = (ssId, questionData, ssName) => {
        setSubSubjectId(ssId);
        setAssessmentQuestion(questionData);
        setSubSubjectName(ssName);
        setPhase('question');
    };

    return (
        <AuthLayout wide>
            <Stepper phase={phase} t={t} />

            {phase === 'survey' && (
                <SurveyForm onComplete={handleSurveyComplete} t={t} />
            )}

            {phase === 'question' && assessmentQuestion && (
                <AssessmentQuestion
                    question={assessmentQuestion}
                    subSubjectId={subSubjectId}
                    subSubjectName={subSubjectName}
                    language={language}
                    onBack={() => setPhase('survey')}
                    onDone={() => setPhase('done')}
                    t={t}
                />
            )}
        </AuthLayout>
    );
};

// ── Step indicator (always LTR: step 1 → step 2) ────────────────────────────

const Stepper = ({ phase, t }) => {
    const surveyDone = phase !== 'survey';
    return (
        <div className="lvl-stepper">
            <Step n={1} label={t.stepAboutYou}   active={phase === 'survey'}   done={surveyDone} />
            <span className={'lvl-connector' + (surveyDone ? ' is-filled' : '')} />
            <Step n={2} label={t.stepAssessment} active={phase === 'question'} done={phase === 'done'} />
        </div>
    );
};

const Step = ({ n, label, active, done }) => (
    <div className="lvl-step">
        <span className={'lvl-dot' + (active ? ' is-active' : done ? ' is-done' : '')}>{done ? '✓' : n}</span>
        <span className={'lvl-step-label' + (active ? ' is-active' : done ? ' is-done' : '')}>{label}</span>
    </div>
);
