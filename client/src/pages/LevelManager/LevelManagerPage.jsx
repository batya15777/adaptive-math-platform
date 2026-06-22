import { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { getLevelManagerStrings } from './levelManagerStrings.js';
import { AuthLayout } from '../../components/auth/AuthLayout.jsx';
import { SurveyForm } from './SurveyForm.jsx';
import { AssessmentQuestion } from './AssessmentQuestion.jsx';
import './levelSurvey.css';

// PHASES: 'survey' → 'question' → 'done'

export const LevelManagerPage = () => {
    const { user }   = useContext(AuthContext);
    const navigate   = useNavigate();
    const { language } = useLanguage();
    const t = getLevelManagerStrings(language);

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
