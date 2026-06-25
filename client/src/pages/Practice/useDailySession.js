import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { getNextQuestion, submitAnswer, revealSolution, claimDailyBonus } from '../../service/progressApi.js';
import { PRACTICE_MODE, getUsedQuestionIds, recordUsedQuestionId } from '../../utils/practiceExclusions.js';
import { format } from '../../i18n/languages.js';
import {
    bumpDailyProgress, readDailySession, writeDailySession, clearDailySession,
} from '../Home/homeLocal.js';
import { orderDailyTopics, pickFreshQuestion, DAILY_GOAL } from './dailyLogic.js';

const MAX_ATTEMPTS = 3;

// Orchestrates a daily-practice session on top of the existing adaptive engine
// (progressApi.getNextQuestion / submitAnswer). Questions are mixed across the
// student's topics (dailyLogic) and stay level-appropriate because getNextQuestion
// already returns a question matched to the student's level for each topic.
//
// • No repeats: every question shown is recorded (`seen`); when the engine returns a
//   question that was already shown, we try the next topic / re-roll until a fresh one
//   appears (capped, with a graceful fallback so we never get stuck).
// • Resumable: the current question + progress + seen-set are persisted (homeLocal) so a
//   page refresh continues exactly where the student was.
//
// phase: 'loading' | 'answering' | 'concluded' | 'done' | 'error'
//   (an empty topic list is reported by the caller, not by this hook)
export function useDailySession({ topics, language, qt, userId, goal = DAILY_GOAL }) {
    const ordered = useMemo(() => orderDailyTopics(topics), [topics]);

    // Restore today's in-progress question (if any) so a refresh doesn't lose it.
    const saved = useMemo(() => (userId != null ? readDailySession(userId) : null), [userId]);

    const [index, setIndex]       = useState(saved?.index ?? 0);
    const [topic, setTopic]       = useState(saved?.topic ?? null);
    const [question, setQuestion] = useState(saved?.question ?? null);
    const [attempt, setAttempt]   = useState(saved?.attempt ?? 1);
    const [correct, setCorrect]   = useState(saved?.correct ?? 0);
    const [phase, setPhase]       = useState(saved?.question ? 'answering' : 'loading');
    const [feedback, setFeedback] = useState(null);
    const [busy, setBusy]         = useState(false);
    const [totalStars, setTotalStars] = useState(saved?.totalStars ?? null);
    const [bonusAwarded, setBonusAwarded] = useState(null); // null=pending, true/false after the claim
    const [error, setError]       = useState('');

    const correctRef = useRef(saved?.correct ?? 0); // mirrors `correct` for the async submit closure
    // Question ids already shown this session — used to guarantee no repeats.
    const seenRef = useRef(new Set(saved?.seen || (saved?.question ? [saved.question.questionId] : [])));

    const showQuestion = useCallback((tp, q) => {
        seenRef.current.add(q.questionId);
        // Claim this id for Daily today so Recommended Practice will exclude it.
        recordUsedQuestionId(PRACTICE_MODE.DAILY, userId, q.questionId);
        setTopic(tp); setQuestion(q);
        setAttempt(1); setFeedback(null); setError('');
        setPhase('answering'); setBusy(false);
    }, [userId]);

    // Loads a *fresh* (unseen) question for slot `startIdx`, re-rolling across topics if
    // the engine returns one already shown (see pickFreshQuestion). State updates happen
    // in the promise callbacks, so this is safe to call straight from an effect.
    const loadQuestion = useCallback((startIdx) => {
        if (ordered.length === 0) return undefined;
        // Exclude whatever Recommended Practice has already used today, so the two never overlap.
        const excludeIds = getUsedQuestionIds(PRACTICE_MODE.RECOMMENDED, userId);
        return pickFreshQuestion({
            orderedTopics: ordered,
            startIndex: startIdx,
            seen: seenRef.current,
            fetch: (id) => getNextQuestion(id, language, excludeIds).then(res => res.data),
        })
            .then(picked => {
                if (picked) showQuestion(picked.topic, picked.question);
                else { setPhase('error'); setBusy(false); }
            })
            .catch(() => { setError(qt.errLoadQuestion); setPhase('error'); setBusy(false); });
    }, [ordered, language, qt, showQuestion, userId]);

    // First question once topics are available — unless we restored one from storage.
    useEffect(() => {
        if (ordered.length === 0) return;
        if (saved?.question) return;
        loadQuestion(0);
    }, [ordered, loadQuestion, saved]);

    // Persist the live session so a refresh can resume it (write-only → no setState here).
    useEffect(() => {
        if (userId == null) return;
        if (phase === 'done') { clearDailySession(userId); return; }
        if (question && (phase === 'answering' || phase === 'concluded')) {
            writeDailySession(userId, {
                index, correct, topic, question, attempt, totalStars,
                seen: Array.from(seenRef.current),
            });
        }
    }, [userId, phase, question, index, correct, topic, attempt, totalStars]);

    const answer = useCallback((given) => {
        if (busy || phase !== 'answering' || !question || !topic) return undefined;
        setBusy(true); setError('');
        return submitAnswer({
            subSubjectId: Number(topic.subSubjectId),
            questionId: question.questionId,
            userAnswer: given,
            questionType: (topic.name || '').toUpperCase(),
            currentDifficulty: question.difficultyLevel,
            attemptNumber: attempt,
            dailyPractice: true, // suppress the per-question reward — daily grants only the +100 bonus
        })
            .then(res => {
                const data = res.data;
                if (data.totalStars != null) setTotalStars(data.totalStars);

                if (data.answerCorrect) {
                    const nextCorrect = correctRef.current + 1;
                    correctRef.current = nextCorrect;
                    setCorrect(nextCorrect);
                    if (userId != null) bumpDailyProgress(userId, goal); // persist partial progress
                    setFeedback({ type: 'correct', text: qt.correct });
                    if (nextCorrect >= goal) {
                        setPhase('done');
                        // Claim the server-verified daily bonus (+100 stars, once/day). The
                        // server re-checks "5 correct today", so this can't be forged.
                        claimDailyBonus()
                            .then(r => {
                                if (r?.data?.totalStars != null) setTotalStars(r.data.totalStars);
                                setBonusAwarded(!!r?.data?.awarded);
                            })
                            .catch(() => setBonusAwarded(false));
                    } else {
                        setPhase('concluded');
                    }
                } else if (data.concluded === 'FAILED') {
                    setFeedback({ type: 'failed', text: qt.outOfTries });
                    setPhase('concluded');
                } else {
                    const nextAttempt = attempt + 1;
                    setAttempt(nextAttempt);
                    setFeedback({ type: 'wrong', text: format(qt.tryAgain, { current: nextAttempt, max: MAX_ATTEMPTS }) });
                    // stays in 'answering' for a retry
                }
            })
            .catch(() => setError(qt.errSubmit))
            .finally(() => setBusy(false));
    }, [busy, phase, question, topic, attempt, qt, userId, goal]);

    // "Guided solution" — give up on the current question and reveal the worked steps.
    // Reuses the existing reveal-solution endpoint (marks it FAILED server-side, same as
    // regular practice); the question concludes WITHOUT counting toward the daily goal, so
    // the student answers another to reach 5. The footer renders the steps on 'failed'.
    const reveal = useCallback(() => {
        if (busy || phase !== 'answering' || !question || !topic) return undefined;
        setBusy(true); setError('');
        return revealSolution({
            subSubjectId: Number(topic.subSubjectId),
            questionId: question.questionId,
        })
            .then(res => {
                if (res?.data?.totalStars != null) setTotalStars(res.data.totalStars);
                setFeedback({ type: 'failed', text: qt.solutionShown });
                setPhase('concluded');
            })
            .catch(() => setError(qt.errRevealSolution))
            .finally(() => setBusy(false));
    }, [busy, phase, question, topic, qt]);

    // Advance to the next question (user-initiated, so synchronous setState is fine here).
    const next = useCallback(() => {
        const i = index + 1;
        setIndex(i);
        setBusy(true); setPhase('loading');
        loadQuestion(i);
    }, [index, loadQuestion]);

    return { question, topic, phase, feedback, busy, error, correct, totalStars, bonusAwarded, goal, answer, reveal, next };
}
