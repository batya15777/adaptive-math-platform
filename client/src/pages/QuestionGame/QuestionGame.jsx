import { useState, useEffect, useCallback, useMemo, useRef, useContext } from 'react';
import Confetti from 'react-confetti';
import _LottieImport from 'lottie-react';
// lottie-react ships CJS only; Vite may hand us the module namespace object instead of
// the default export. This unwrap handles both the pre-bundled case (function) and the
// not-yet-pre-bundled case (object with .default).
const Lottie = typeof _LottieImport === 'function' ? _LottieImport : _LottieImport.default;
import trophyAnimation from '../../assets/bonusStars/Trophy.json';
import './QuestionGame.css';
import '../../styles/spaceTokens.css';
import { TutorChatLauncher } from '../../components/TutorChat/TutorChatLauncher.jsx';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import {
    getStatus, getNextQuestion, submitAnswer, revealSolution,
    getBonusQuestion, submitBonusAnswer,
} from '../../service/progressApi.js';
import { getMyCluster } from '../../service/dashboardApi.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { useProfile } from '../../contexts/useProfile.js';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { PRACTICE_MODE, getUsedQuestionIds, recordUsedQuestionId } from '../../utils/practiceExclusions.js';
import { format } from '../../i18n/languages.js';
import { getQuestionGameStrings } from './questionGameStrings.js';
import { parseSolution } from '../../utils/questionFormat.js';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import { QuestionCard } from '../../components/practice/QuestionCard.jsx';
import { LevelRocketProgress } from '../../components/practice/LevelRocketProgress.jsx';
import { playLevelUpSound } from '../../utils/sound.js';

const MAX_ATTEMPTS = 3;

export const QuestionGame = () => {
    const { subSubjectId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { language, dir } = useLanguage();
    const { profileData } = useProfile();
    const theme = (profileData.theme || 'LIGHT').toLowerCase();
    // Memoized so it stays stable across renders (only changes with language),
    // which keeps the useCallback/effect dependency chains below from re-firing.
    const t = useMemo(() => getQuestionGameStrings(language), [language]);
    const { user } = useContext(AuthContext);
    const subSubjectName = location.state?.subSubjectName || t.practiceFallback;
    const backTopic   = location.state?.topic;    // carried through so "back" returns to
    const backSubject = location.state?.subject;  // the subject's sub-subjects tab
    // Launched from the Home "Recommended for you" card → must not reuse today's Daily questions.
    const isRecommended = location.state?.mode === 'recommended';

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
            // Recommended Practice excludes today's Daily questions; then claims its own id so
            // Daily will exclude it in turn (full two-way separation). Regular training: no excludes.
            const excludeIds = isRecommended ? getUsedQuestionIds(PRACTICE_MODE.DAILY, user?.id) : undefined;
            const res = await getNextQuestion(subSubjectId, language, excludeIds);
            setQuestion(res.data);
            if (isRecommended && res.data?.questionId != null) {
                recordUsedQuestionId(PRACTICE_MODE.RECOMMENDED, user?.id, res.data.questionId);
            }
        } catch {
            setError(t.errLoadQuestion);
        } finally {
            setLoading(false);
        }
    }, [subSubjectId, language, t, isRecommended, user?.id]);

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

    // Hint/solution steps still parsed here — QuestionCard parses the options itself.
    const steps = question ? parseSolution(question.solution) : [];

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
                playLevelUpSound();
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
            // Send the full answer so the server grades it AND records the attempt for the ML
            // pipeline (same shape as a normal submit). Correctness comes back from the server.
            const res = await submitBonusAnswer({
                subSubjectId: Number(subSubjectId),
                questionId: question.questionId,
                userAnswer: given,
                questionType: subSubjectName.toUpperCase(),
                currentDifficulty: question.difficultyLevel,
            });
            const correct = res.data.answerCorrect;

            if (res.data.currentLevel > progress.currentLevel) {
                clearTimeout(confettiTimerRef.current);
                setShowLevelUpConfetti(true);
                confettiTimerRef.current = setTimeout(() => setShowLevelUpConfetti(false), 4500);
                playLevelUpSound();
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

    // ── render ──────────────────────────────────────────────────────────────
    // Extra controls under the shared QuestionCard (staged hints, solution, next) —
    // passed through the card's `footer` slot so the card itself stays generic.
    const cardFooter = question && (
        <>
            {!isBonus && (
                <div className="qg-actions">
                    <button type="button" className="sc-btn sc-btn--ghost" onClick={() => setRevealedHints(n => n + 1)} disabled={hintDisabled}>
                        {t.hint}
                    </button>
                    <button type="button" className="sc-btn sc-btn--ghost" onClick={handleRevealSolution} disabled={concluded || loading}>
                        {t.showSolution}
                    </button>
                </div>
            )}

            {(revealedHints > 0 || showSolution) && steps.length > 0 && (
                <ol className="qc-solution qc-math">
                    {(showSolution ? steps : steps.slice(0, revealedHints)).map((step, i) => (
                        <li key={i}>{step}</li>
                    ))}
                </ol>
            )}

            {concluded && (
                <button type="button" className="sc-btn qg-next" onClick={handleNext} disabled={loading}>
                    {pendingBonus ? t.goToBonus : t.nextQuestion}
                </button>
            )}
        </>
    );

    return (
        <div className="mg-space qg-root" data-theme={theme} dir={dir}>
            <Stars />

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
                <div className="qg-trophy">
                    <Lottie animationData={trophyAnimation} loop={false} style={{ width: 320, height: 320 }} />
                </div>
            )}

            <div className="sc-content qg-topbar">
                <AppTopBar onBack={() => navigate('/math-training', { state: { topic: backTopic, subject: backSubject } })} />
            </div>

            <div className="sc-content qg-inner">
                <div className="qg-head">
                    <h1 className="qg-title">{subSubjectName}</h1>
                    <div className="qg-badges">
                        {cluster && (
                            <span className="qg-badge qg-badge--adapt" title={format(t.adaptedTitle, { label: cluster.label || '' })}>
                                {t.adaptedForYou}
                            </span>
                        )}
                        {progress.currentStreak >= 10 && (
                            <span className="qg-badge qg-badge--streak">{format(t.streak, { count: progress.currentStreak })}</span>
                        )}
                        <span className="qg-badge qg-badge--star" dir="ltr">⭐ {progress.totalStars}</span>
                    </div>
                </div>

                {isBonus && <div className="qg-bonus-banner">{t.bonusBanner}</div>}
                {error && <p className="qg-msg qg-msg--err">{error}</p>}

                <div className={`sc-card qg-card${shaking ? ' shake' : ''}`}>
                    {question ? (
                        <QuestionCard
                            key={question.questionId}
                            question={question}
                            t={t}
                            feedback={feedback}
                            locked={concluded || loading}
                            onAnswer={handleAnswer}
                            meta={!isBonus && !concluded ? format(t.attempt, { current: attemptNumber, max: MAX_ATTEMPTS }) : null}
                            showSolution={false}
                            footer={cardFooter}
                        />
                    ) : (
                        loading && <p className="qg-msg">{t.loading}</p>
                    )}
                </div>

                {/* Rocket journey to the next level — advances only on correct answers. */}
                <LevelRocketProgress
                    currentLevel={progress.currentLevel}
                    current={progress.levelProgressCurrent}
                    target={progress.levelProgressTarget}
                    inSubLevel={progress.inSubLevel}
                    celebrate={showLevelUpConfetti}
                    t={t}
                />
            </div>

            {/* Existing AI tutor as a floating launcher — closed by default, opens on click;
                keyed by questionId so the chat resets each question. Hidden for bonus mode. */}
            {question && !isBonus && (
                <TutorChatLauncher key={question.questionId} questionId={question.questionId} language={language} />
            )}
        </div>
    );
};

// All visual styling now lives in QuestionGame.css (themed via the shared --mg-* tokens),
// matching the daily-practice look. The shake keyframes also live there.
