import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import Confetti from 'react-confetti';
import _LottieImport from 'lottie-react';
// lottie-react ships CJS only; Vite may hand us the module namespace object instead of
// the default export. This unwrap handles both the pre-bundled case (function) and the
// not-yet-pre-bundled case (object with .default).
const Lottie = typeof _LottieImport === 'function' ? _LottieImport : _LottieImport.default;
import levelUpSound from '../../assets/sound/level-up.mp3';
import trophyAnimation from '../../assets/bonusStars/Trophy.json';
import './QuestionGame.css';
import TutorChat from '../../components/TutorChat/TutorChat.jsx';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import {
    getStatus, getNextQuestion, submitAnswer, revealSolution,
    getBonusQuestion, submitBonusAnswer,
} from '../../service/progressApi.js';
import { getMyCluster } from '../../service/dashboardApi.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getQuestionGameStrings } from './questionGameStrings.js';

const MAX_ATTEMPTS = 3;

const LANG_CODE = { HEBREW: 'he', ENGLISH: 'en', RUSSIAN: 'ru' };

// "5|7|9|11" → ["5","7","9","11"];  "" / null → []
// Pipe-delimited so an option may contain commas (e.g. "X1=5 , X2=10").
const parseOptions  = (s) => (s ? s.split('|').map(o => o.trim()).filter(Boolean) : []);
// "a = 1\nb = 2" → ["a = 1","b = 2"]
const parseSolution = (s) => (s ? s.split('\n').map(t => t.trim()).filter(Boolean) : []);

export const QuestionGame = () => {
    const { subSubjectId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { language } = useLanguage();
    // Memoized so it stays stable across renders (only changes with language),
    // which keeps the useCallback/effect dependency chains below from re-firing.
    const t = useMemo(() => getQuestionGameStrings(language), [language]);
    const subSubjectName = location.state?.subSubjectName || t.practiceFallback;
    const backTopic   = location.state?.topic;    // carried through so "back" returns to
    const backSubject = location.state?.subject;  // the subject's sub-subjects tab

    const [question, setQuestion] = useState(null);
    const [isBonus,  setIsBonus]  = useState(false);
    const [cluster,  setCluster]  = useState(null); // ML cohort, for the "adapted for you" badge

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
        currentStreak: 0,
    });

    const [loading, setLoading] = useState(false);
    const [error,   setError]   = useState('');

    const [showLevelUpConfetti, setShowLevelUpConfetti] = useState(false);
    const [shaking,             setShaking]             = useState(false);
    const [showTrophy,          setShowTrophy]          = useState(false);
    const confettiTimerRef = useRef(null);
    const shakeTimerRef    = useRef(null);
    const trophyTimerRef   = useRef(null);

    useEffect(() => () => {
        clearTimeout(confettiTimerRef.current);
        clearTimeout(shakeTimerRef.current);
        clearTimeout(trophyTimerRef.current);
    }, []);

    // Create a fresh Audio object on every call — avoids browser autoplay-policy issues
    // that can block a stored Audio ref from playing after an async gap.
    const playLevelUpAudio = useCallback(() => {
        const audio = new Audio(levelUpSound);
        audio.play().catch(() => {});
    }, []);

    // Merge a server status into the bar. The server preserves level progress during the
    // sub-level (it only advances on normal-level correct answers), so the bar freezes on its own.
    const applyProgress = useCallback((data) => {
        setProgress({
            currentLevel: data.currentLevel,
            totalStars:   data.totalStars,
            inSubLevel:   data.inSubLevel,
            levelProgressTarget:  data.levelProgressTarget || 24,
            levelProgressCurrent: data.levelProgressCurrent,
            currentStreak: data.currentStreak || 0,
        });
    }, []);

    const resetPerQuestion = () => {
        setAnswer(''); setAttemptNumber(1); setRevealedHints(0);
        setShowSolution(false); setConcluded(false); setFeedback(null);
        setShaking(false); setShowTrophy(false);
        clearTimeout(trophyTimerRef.current);
    };

    const loadRegularQuestion = useCallback(async () => {
        setLoading(true); setError('');
        try {
            resetPerQuestion();
            setIsBonus(false);
            const res = await getNextQuestion(subSubjectId, language);
            setQuestion(res.data);
        } catch {
            setError(t.errLoadQuestion);
        } finally {
            setLoading(false);
        }
    }, [subSubjectId, language, t]);

    const loadBonusQuestion = useCallback(async () => {
        setLoading(true); setError('');
        try {
            resetPerQuestion();
            setIsBonus(true);
            setPendingBonus(false);
            const res = await getBonusQuestion(subSubjectId, language);
            setQuestion(res.data);
        } catch {
            setError(t.errLoadBonus);
        } finally {
            setLoading(false);
        }
    }, [subSubjectId, language, t]);

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

    // Fetch the student's ML cohort once, to show an "adapted for you" badge.
    useEffect(() => {
        let active = true;
        getMyCluster()
            .then(res => { if (active && res.data?.assigned) setCluster(res.data); })
            .catch(() => {});
        return () => { active = false; };
    }, []);

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

            // Level-up detection — compare BEFORE applyProgress updates the state
            if (data.currentLevel > progress.currentLevel) {
                clearTimeout(confettiTimerRef.current);
                setShowLevelUpConfetti(true);
                confettiTimerRef.current = setTimeout(() => setShowLevelUpConfetti(false), 4500);
                playLevelUpAudio();
            }

            applyProgress(data);

            if (data.answerCorrect) {
                setFeedback({ type: 'correct', text: data.message || t.correct });
                setConcluded(true);
                if (data.bonusQuestionTriggered) setPendingBonus(true);
            } else {
                console.log('Wrong Answer Detected - Triggering Shake');
                clearTimeout(shakeTimerRef.current);
                setShaking(true);
                shakeTimerRef.current = setTimeout(() => setShaking(false), 600);

                if (data.concluded === 'FAILED') {
                    setShowSolution(true);
                    setConcluded(true);
                    setFeedback({ type: 'failed', text: data.message || t.outOfTries });
                } else {
                    setAttemptNumber(n => n + 1);
                    setAnswer('');
                    setFeedback({ type: 'wrong', text: format(t.tryAgain, { current: attemptNumber + 1, max: MAX_ATTEMPTS }) });
                }
            }
        } catch {
            setError(t.errSubmit);
        } finally {
            setLoading(false);
        }
    };

    const handleBonusAnswer = async (given) => {
        setLoading(true); setError('');
        try {
            const correct = given === (question.correctAnswer ?? '').toString().trim();
            const res = await submitBonusAnswer(Number(subSubjectId), correct);

            if (res.data.currentLevel > progress.currentLevel) {
                clearTimeout(confettiTimerRef.current);
                setShowLevelUpConfetti(true);
                confettiTimerRef.current = setTimeout(() => setShowLevelUpConfetti(false), 4500);
                playLevelUpAudio();
            }

            applyProgress(res.data);
            setConcluded(true);
            if (correct) {
                console.log('Correct Bonus Answer Detected - Fetching Animation');
                setFeedback({ type: 'correct', text: t.bonusSolved });
                setShowTrophy(true);
                // Auto-advance after the animation plays — clears the overlay AND loads next question
                clearTimeout(trophyTimerRef.current);
                trophyTimerRef.current = setTimeout(() => {
                    setShowTrophy(false);
                    loadRegularQuestion();
                }, 3500);
            } else {
                console.log('Wrong Answer Detected - Triggering Shake');
                clearTimeout(shakeTimerRef.current);
                setShaking(true);
                shakeTimerRef.current = setTimeout(() => setShaking(false), 600);
                setFeedback({ type: 'failed', text: t.bonusMissed });
                setShowSolution(true);
            }
        } catch {
            setError(t.errSubmitBonus);
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
            setFeedback({ type: 'failed', text: t.solutionShown });
        } catch {
            setError(t.errRevealSolution);
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
        <>
        {showLevelUpConfetti && (
            <Confetti
                width={window.innerWidth}
                height={window.innerHeight}
                recycle={false}
                numberOfPieces={400}
                gravity={0.22}
                style={{ position: 'fixed', top: 0, left: 0, zIndex: 9999, pointerEvents: 'none' }}
            />
        )}
        {showTrophy && (
            <div style={trophyOverlay}>
                <Lottie
                    animationData={trophyAnimation}
                    loop={false}
                    style={{ width: 320, height: 320 }}
                />
            </div>
        )}
        <div style={page}>
            <div style={topBar}>
                <button
                    onClick={() => navigate('/math-training', { state: { topic: backTopic, subject: backSubject } })}
                    style={btn('#6c757d')}
                >
                    ← {backSubject?.name || t.back}
                </button>
                <h2 style={{ margin: 0, color: '#333', textTransform: 'capitalize' }}>{subSubjectName}</h2>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {cluster && (
                        <span style={adaptedBadge} title={format(t.adaptedTitle, { label: cluster.label || '' })}>
                            {t.adaptedForYou}
                        </span>
                    )}
                    {progress.currentStreak >= 10 && (
                        <span style={streakBadge}>{format(t.streak, { count: progress.currentStreak })}</span>
                    )}
                    <span style={starBadge}>⭐ {progress.totalStars}</span>
                </div>
            </div>

            {error && <p style={{ color: '#dc3545' }}>{error}</p>}

            {isBonus && (
                <div style={bonusBanner}>{t.bonusBanner}</div>
            )}

            {question ? (
                <div style={card} className={shaking ? 'shake' : ''}>
                    <div style={{ fontSize: '13px', color: '#888', marginBottom: '6px' }}>
                        {format(t.difficulty, { level: question.difficultyLevel })}
                        {!isBonus && !concluded && ` · ${format(t.attempt, { current: attemptNumber, max: MAX_ATTEMPTS })}`}
                    </div>

                    {/* Math stays LTR even when the page is RTL (Hebrew). */}
                    <div dir="ltr" style={expression}>{question.expression}</div>

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
                                placeholder={t.answerPlaceholder}
                                style={input}
                            />
                            <button
                                disabled={concluded || loading || !answer.trim()}
                                onClick={() => handleAnswer()}
                                style={btn('#28a745')}
                            >
                                {t.submit}
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
                                {t.hint}
                            </button>
                            <button onClick={handleRevealSolution} disabled={concluded || loading} style={btn(concluded ? '#adb5bd' : '#fd7e14')}>
                                {t.showSolution}
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
                            {pendingBonus ? t.goToBonus : t.nextQuestion}
                        </button>
                    )}
                </div>
            ) : (
                loading && <p style={{ color: '#888' }}>{t.loading}</p>
            )}

            {/* progress to next level */}
            <div style={{ marginTop: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#555', marginBottom: '4px' }}>
                    <span>{format(t.level, { level: progress.currentLevel })}{progress.inSubLevel && t.subLevel}</span>
                    <span>{format(t.toNextLevel, { current: progress.levelProgressCurrent, target: progress.levelProgressTarget })}</span>
                </div>
                <div style={barTrack}>
                    <div style={{ ...barFill, width: `${barPct}%`, backgroundColor: progress.inSubLevel ? '#fd7e14' : '#28a745' }} />
                </div>
                {progress.inSubLevel && (
                    <p style={{ fontSize: '12px', color: '#fd7e14', marginTop: '4px' }}>
                        {t.subLevelPaused}
                    </p>
                )}
            </div>

            {/* AI tutor for the current exercise. Keyed by questionId so the chat
                resets when the student moves to the next question. Hidden for bonus
                questions, which are a separate high-stakes mode. */}
            {question && !isBonus && (
                <div style={{ marginTop: '24px' }}>
                    <TutorChat key={question.questionId} questionId={question.questionId} language={language} />
                </div>
            )}
        </div>
        </>
    );
};

// ── styles ──────────────────────────────────────────────────────────────────
const feedbackColor = (t) => (t === 'correct' ? '#28a745' : t === 'wrong' ? '#dc3545' : '#fd7e14');

const page = { padding: '20px', maxWidth: '640px', margin: '0 auto', fontFamily: 'Arial, sans-serif' };

const topBar = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', marginBottom: '16px' };

const starBadge   = { fontSize: '15px', fontWeight: 'bold', color: '#f0ad4e' };
const adaptedBadge = {
    fontSize: '12px', fontWeight: 'bold', color: '#fff',
    backgroundColor: '#007bff', borderRadius: '20px',
    padding: '4px 10px', whiteSpace: 'nowrap',
};
const streakBadge = {
    fontSize: '14px', fontWeight: 'bold', color: '#fff',
    backgroundColor: '#fd7e14', borderRadius: '20px',
    padding: '4px 10px', whiteSpace: 'nowrap',
    boxShadow: '0 0 8px rgba(253,126,20,0.5)',
};

const trophyOverlay = {
    position: 'fixed', inset: 0, zIndex: 9998,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    pointerEvents: 'none',
};

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
