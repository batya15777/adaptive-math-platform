import { useState, useEffect, useCallback } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useProfile } from '../../contexts/useProfile.js';
import {
    getStatus, getNextQuestion, submitAnswer, revealSolution,
    getBonusQuestion, submitBonusAnswer,
} from '../../service/progressApi.js';

const MAX_ATTEMPTS = 3;

const LANG_CODE = { HEBREW: 'he', ENGLISH: 'en', RUSSIAN: 'ru' };

// "5,7,9,11" → ["5","7","9","11"];  "" / null → []
const parseOptions  = (s) => (s ? s.split(',').map(o => o.trim()).filter(Boolean) : []);
// "a = 1\nb = 2" → ["a = 1","b = 2"]
const parseSolution = (s) => (s ? s.split('\n').map(t => t.trim()).filter(Boolean) : []);

export const QuestionGame = () => {
    const { subSubjectId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { profileData } = useProfile();
    const language = LANG_CODE[profileData?.language] || 'he';
    const subSubjectName = location.state?.subSubjectName || 'Practice';
    const backTopic   = location.state?.topic;    // carried through so "back" returns to
    const backSubject = location.state?.subject;  // the subject's sub-subjects tab

    const [question, setQuestion] = useState(null);
    const [isBonus,  setIsBonus]  = useState(false);

    const [answer,        setAnswer]        = useState('');
    const [attemptNumber, setAttemptNumber] = useState(1);
    const [revealedHints, setRevealedHints] = useState(0);
    const [showSolution,  setShowSolution]  = useState(false);
    const [concluded,     setConcluded]     = useState(false);
    const [pendingBonus,  setPendingBonus]  = useState(false);
    const [feedback,      setFeedback]      = useState(null); // {type, text}

    const [progress, setProgress] = useState({
        currentLevel: 1, inSubLevel: false,
        levelProgressCurrent: 0, levelProgressTarget: 24, totalStars: 0,
    });

    const [loading, setLoading] = useState(false);
    const [error,   setError]   = useState('');

    // Merge a server status into the bar. The server preserves level progress during the
    // sub-level (it only advances on normal-level correct answers), so the bar freezes on its own.
    const applyProgress = useCallback((data) => {
        setProgress({
            currentLevel: data.currentLevel,
            totalStars:   data.totalStars,
            inSubLevel:   data.inSubLevel,
            levelProgressTarget:  data.levelProgressTarget || 24,
            levelProgressCurrent: data.levelProgressCurrent,
        });
    }, []);

    const resetPerQuestion = () => {
        setAnswer(''); setAttemptNumber(1); setRevealedHints(0);
        setShowSolution(false); setConcluded(false); setFeedback(null);
    };

    const loadRegularQuestion = useCallback(async () => {
        setLoading(true); setError('');
        try {
            resetPerQuestion();
            setIsBonus(false);
            const res = await getNextQuestion(subSubjectId, language);
            setQuestion(res.data);
        } catch {
            setError('Could not load the question. Please try again.');
        } finally {
            setLoading(false);
        }
    }, [subSubjectId, language]);

    const loadBonusQuestion = useCallback(async () => {
        setLoading(true); setError('');
        try {
            resetPerQuestion();
            setIsBonus(true);
            setPendingBonus(false);
            const res = await getBonusQuestion(subSubjectId, language);
            setQuestion(res.data);
        } catch {
            setError('Could not load the bonus question.');
        } finally {
            setLoading(false);
        }
    }, [subSubjectId, language]);

    // Initial load: status (bar) + first question
    useEffect(() => {
        let active = true;
        (async () => {
            setLoading(true);
            try {
                const status = await getStatus(subSubjectId);
                if (active) applyProgress(status.data);
            } catch { /* fall back to defaults */ }
            if (active) await loadRegularQuestion();
        })();
        return () => { active = false; };
    }, [subSubjectId, applyProgress, loadRegularQuestion]);

    const options = question ? parseOptions(question.options) : [];
    const steps   = question ? parseSolution(question.solution) : [];
    const isMultipleChoice = options.length > 0;

    // ── answering ─────────────────────────────────────────────────────────────
    const handleAnswer = async (value) => {
        if (!question || concluded || loading) return;
        const given = (value ?? answer).toString().trim();
        if (!given) return;

        if (isBonus) return handleBonusAnswer(given);

        setLoading(true); setError('');
        try {
            const res = await submitAnswer({
                subSubjectId: Number(subSubjectId),
                questionId: question.questionId,
                userAnswer: given,
                questionType: subSubjectName.toUpperCase(),
                currentDifficulty: question.difficultyLevel,
                attemptNumber,
            });
            const data = res.data;
            applyProgress(data);

            if (data.answerCorrect) {
                setFeedback({ type: 'correct', text: data.message || 'Correct! ✅' });
                setConcluded(true);
                if (data.bonusQuestionTriggered) setPendingBonus(true);
            } else if (data.concluded === 'FAILED') {
                setShowSolution(true);
                setConcluded(true);
                setFeedback({ type: 'failed', text: data.message || 'Out of tries — here is the solution.' });
            } else {
                setAttemptNumber(n => n + 1);
                setAnswer('');
                setFeedback({ type: 'wrong', text: `Not quite — try again (${attemptNumber + 1}/${MAX_ATTEMPTS}).` });
            }
        } catch {
            setError('Could not submit your answer. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleBonusAnswer = async (given) => {
        setLoading(true); setError('');
        try {
            const correct = given === (question.correctAnswer ?? '').toString().trim();
            const res = await submitBonusAnswer(Number(subSubjectId), correct);
            applyProgress(res.data);
            setConcluded(true);
            setFeedback(correct
                ? { type: 'correct', text: 'Bonus solved! +50 ⭐' }
                : { type: 'failed', text: 'Bonus missed — no stars this time.' });
            if (!correct) setShowSolution(true);
        } catch {
            setError('Could not submit the bonus answer.');
        } finally {
            setLoading(false);
        }
    };

    const handleRevealSolution = async () => {
        if (!question || concluded || loading) return;
        setLoading(true); setError('');
        try {
            const res = await revealSolution({
                subSubjectId: Number(subSubjectId),
                questionId: question.questionId,
            });
            applyProgress(res.data);
            setShowSolution(true);
            setConcluded(true);
            setFeedback({ type: 'failed', text: 'Solution shown — this one is marked as failed.' });
        } catch {
            setError('Could not reveal the solution.');
        } finally {
            setLoading(false);
        }
    };

    const handleNext = () => {
        if (pendingBonus) loadBonusQuestion();
        else loadRegularQuestion();
    };

    // hint button is disabled with ≤1 step, or once only the final step is left unrevealed
    const hintDisabled = steps.length <= 1 || revealedHints >= steps.length - 1 || concluded;

    const barPct = Math.min(100, Math.round(
        (progress.levelProgressCurrent / Math.max(1, progress.levelProgressTarget)) * 100));

    // ── render ──────────────────────────────────────────────────────────────
    return (
        <div style={page}>
            <div style={topBar}>
                <button
                    onClick={() => navigate('/math-training', { state: { topic: backTopic, subject: backSubject } })}
                    style={btn('#6c757d')}
                >
                    ← {backSubject?.name || 'Back'}
                </button>
                <h2 style={{ margin: 0, color: '#333', textTransform: 'capitalize' }}>{subSubjectName}</h2>
                <span style={starBadge}>⭐ {progress.totalStars}</span>
            </div>

            {error && <p style={{ color: '#dc3545' }}>{error}</p>}

            {isBonus && (
                <div style={bonusBanner}>🌟 Bonus question — harder, worth 50 stars!</div>
            )}

            {question ? (
                <div style={card}>
                    <div style={{ fontSize: '13px', color: '#888', marginBottom: '6px' }}>
                        Difficulty {question.difficultyLevel}
                        {!isBonus && !concluded && ` · Attempt ${attemptNumber}/${MAX_ATTEMPTS}`}
                    </div>

                    <div style={expression}>{question.expression}</div>

                    {/* answer area */}
                    {isMultipleChoice ? (
                        <div style={optionGrid}>
                            {options.map((opt) => (
                                <button
                                    key={opt}
                                    disabled={concluded || loading}
                                    onClick={() => handleAnswer(opt)}
                                    style={optionBtn(concluded)}
                                >
                                    {opt}
                                </button>
                            ))}
                        </div>
                    ) : (
                        <div style={{ display: 'flex', gap: '10px', marginTop: '12px' }}>
                            <input
                                type="text"
                                value={answer}
                                disabled={concluded || loading}
                                onChange={e => setAnswer(e.target.value)}
                                onKeyDown={e => { if (e.key === 'Enter') handleAnswer(); }}
                                placeholder="Type your answer"
                                style={input}
                            />
                            <button
                                disabled={concluded || loading || !answer.trim()}
                                onClick={() => handleAnswer()}
                                style={btn('#28a745')}
                            >
                                Submit
                            </button>
                        </div>
                    )}

                    {feedback && (
                        <p style={{ marginTop: '12px', fontWeight: 'bold', color: feedbackColor(feedback.type) }}>
                            {feedback.text}
                        </p>
                    )}

                    {/* hints / solution */}
                    {!isBonus && (
                        <div style={{ display: 'flex', gap: '10px', marginTop: '12px', flexWrap: 'wrap' }}>
                            <button onClick={() => setRevealedHints(n => n + 1)} disabled={hintDisabled} style={btn(hintDisabled ? '#adb5bd' : '#007bff')}>
                                💡 Hint
                            </button>
                            <button onClick={handleRevealSolution} disabled={concluded || loading} style={btn(concluded ? '#adb5bd' : '#fd7e14')}>
                                Show full solution
                            </button>
                        </div>
                    )}

                    {/* revealed steps (hints) or full solution */}
                    {(revealedHints > 0 || showSolution) && steps.length > 0 && (
                        <ol style={solutionBox}>
                            {(showSolution ? steps : steps.slice(0, revealedHints)).map((step, i) => (
                                <li key={i} style={{ marginBottom: '4px' }}>{step}</li>
                            ))}
                        </ol>
                    )}

                    {concluded && (
                        <button onClick={handleNext} disabled={loading} style={{ ...btn('#007bff'), marginTop: '16px', width: '100%' }}>
                            {pendingBonus ? 'Go to bonus question →' : 'Next question →'}
                        </button>
                    )}
                </div>
            ) : (
                loading && <p style={{ color: '#888' }}>Loading...</p>
            )}

            {/* progress to next level */}
            <div style={{ marginTop: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#555', marginBottom: '4px' }}>
                    <span>Level {progress.currentLevel}{progress.inSubLevel && ' · Sub-level (easier practice)'}</span>
                    <span>{progress.levelProgressCurrent}/{progress.levelProgressTarget} to next level</span>
                </div>
                <div style={barTrack}>
                    <div style={{ ...barFill, width: `${barPct}%`, backgroundColor: progress.inSubLevel ? '#fd7e14' : '#28a745' }} />
                </div>
                {progress.inSubLevel && (
                    <p style={{ fontSize: '12px', color: '#fd7e14', marginTop: '4px' }}>
                        Solve a few easier questions to get back to your level — the bar is paused.
                    </p>
                )}
            </div>
        </div>
    );
};

// ── styles ──────────────────────────────────────────────────────────────────
const feedbackColor = (t) => (t === 'correct' ? '#28a745' : t === 'wrong' ? '#dc3545' : '#fd7e14');

const page = { padding: '20px', maxWidth: '640px', margin: '0 auto', fontFamily: 'Arial, sans-serif' };

const topBar = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', marginBottom: '16px' };

const starBadge = { fontSize: '15px', fontWeight: 'bold', color: '#f0ad4e' };

const bonusBanner = {
    padding: '10px 14px', marginBottom: '12px', borderRadius: '6px',
    backgroundColor: '#fff3cd', color: '#856404', border: '1px solid #ffeeba', fontWeight: 'bold',
};

const card = { border: '1px solid #ddd', borderRadius: '8px', padding: '24px', backgroundColor: '#f9f9f9' };

const expression = {
    fontSize: '32px', fontWeight: 'bold', color: '#222', textAlign: 'center',
    padding: '12px 0', letterSpacing: '1px',
};

const optionGrid = { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginTop: '12px' };

const optionBtn = (disabled) => ({
    padding: '14px', fontSize: '18px', fontWeight: 'bold',
    backgroundColor: disabled ? '#e9ecef' : '#fff', color: '#333',
    border: '2px solid #007bff', borderRadius: '6px',
    cursor: disabled ? 'default' : 'pointer',
});

const input = {
    flex: 1, padding: '12px', fontSize: '18px',
    border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box',
};

const solutionBox = {
    marginTop: '14px', padding: '12px 12px 12px 28px', backgroundColor: '#eef6ff',
    border: '1px solid #cfe2ff', borderRadius: '6px', color: '#333',
};

const barTrack = { width: '100%', height: '16px', backgroundColor: '#e9ecef', borderRadius: '8px', overflow: 'hidden' };
const barFill  = { height: '100%', transition: 'width 0.3s ease' };

const btn = (bg) => ({
    padding: '10px 16px', backgroundColor: bg, color: 'white',
    border: 'none', borderRadius: '4px', cursor: 'pointer',
    fontSize: '14px', fontWeight: 'bold',
});
