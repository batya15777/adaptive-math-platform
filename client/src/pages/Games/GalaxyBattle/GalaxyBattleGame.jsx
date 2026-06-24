import { useState, useEffect, useContext, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContextSetup.js';
import { useProfile } from '../../../contexts/useProfile.js';
import { useLanguage } from '../../../i18n/useLanguage.js';
import { getDashboardData } from '../../../service/dashboardApi.js';
import { startGalaxyBattle, finishGalaxyBattle } from '../../../service/gamesApi.js';
import { createBattleState, applyAnswer } from '../../../utils/battleEngine.js';
import { Stars } from '../../../components/ui/Stars.jsx';
import { AppTopBar } from '../../../components/ui/AppTopBar.jsx';
import { getGamesStrings } from '../gamesStrings.js';
import { TopicPlanetSelector } from './TopicPlanetSelector.jsx';
import { BattleArena } from './BattleArena.jsx';
import { BattleResult } from './BattleResult.jsx';
import { ANSWER_GRACE_SECONDS, MONSTER_WARNING_SECONDS, isGalaxyBattleTopicSupported } from '../../../utils/gameConstants.js';
import '../../../styles/spaceTokens.css';
import './galaxyBattle.css';

// Orchestrates the Galaxy Battle: choose a planet (topic) → pay + start (server) → battle → result.
// All battle math is in battleEngine (pure); this component wires it to the UI + API.
export const GalaxyBattleGame = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { language, dir } = useLanguage();
    const t = getGamesStrings(language);
    const theme = (profileData.theme || 'LIGHT').toLowerCase();
    const navigate = useNavigate();

    const gk = user?.gender === 'male' ? 'male' : user?.gender === 'female' ? 'female' : 'neutral';
    const g = (o) => (o && typeof o === 'object' ? (o[gk] ?? o.neutral) : o) || '';

    const [phase, setPhase] = useState('loading');   // loading | select | intro | battle | result
    const [topics, setTopics] = useState([]);
    const [recommendedId, setRecommendedId] = useState(null);
    const [battle, setBattle] = useState(null);       // { questions, monsterMaxHp, level, topicName, subSubjectId }
    const [bs, setBs] = useState(null);               // battle state (engine)
    const [qIndex, setQIndex] = useState(0);
    const [starsAfter, setStarsAfter] = useState(null); // balance after the 200 deduction (for the HUD)
    const [starting, setStarting] = useState(false);  // waiting on the start API (deduction)
    const [countdown, setCountdown] = useState(null); // 3 | 2 | 1 | 'go' | null
    const [monsterCharge, setMonsterCharge] = useState(null); // null = calm; 10..0 = monster charging
    const [feedback, setFeedback] = useState(null);   // { type, picked, super } during the hit, then advance
    const [locked, setLocked] = useState(false);
    const [notEnough, setNotEnough] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        let active = true;
        getDashboardData()
            .then(r => {
                if (!active) return;
                // Galaxy Battle only: keep only topics the battle generator actually supports, so a
                // battle never serves a wrong-topic question (e.g. "Percentages" → addition). This
                // filter is scoped to the battle — Percentages stays in admin / regular practice / etc.
                const tp = (r.data?.topics || []).filter(x => isGalaxyBattleTopicSupported(x.name));
                setTopics(tp);
                const recs = r.data?.recommendations || [];
                const recId = recs.find(x => x.recommendedSubSubjectId
                        && tp.some(s => s.subSubjectId === x.recommendedSubSubjectId))?.recommendedSubSubjectId
                    || [...tp].sort((a, b) => (a.successRate || 0) - (b.successRate || 0))[0]?.subSubjectId
                    || null;
                setRecommendedId(recId);
                setPhase('select');
            })
            .catch(() => { if (active) { setError(t.errorStart); setPhase('select'); } });
        return () => { active = false; };
    }, [t.errorStart]);

    // Pick a planet → spend the 200 stars (server-side, atomic with building the battle on this
    // topic) and go straight into the fight. No separate confirm screen, and exactly ONE charge:
    // the deduction happens here, once, the moment a topic is chosen (402 → "not enough stars").
    const startBattleWithTopic = (topic) => {
        if (!topic || starting) return;
        setError(''); setNotEnough(false); setStarting(true);
        startGalaxyBattle(topic.subSubjectId, language)
            .then(res => {
                const d = res.data;
                setBattle({
                    questions: d.questions || [],
                    monsterMaxHp: d.monsterMaxHp,
                    level: d.level,
                    topicName: topic.name,
                    subSubjectId: topic.subSubjectId,
                });
                setBs(createBattleState(d.monsterMaxHp));
                setStarsAfter(d.updatedStars);
                setQIndex(0); setFeedback(null);
                setPhase('battle');
                setCountdown(3);
            })
            .catch(err => {
                if (err.response?.status === 402) setNotEnough(true);
                else setError(t.errorStart);
            })
            .finally(() => setStarting(false));
    };

    // 3 → 2 → 1 → FIGHT! → start.
    useEffect(() => {
        if (countdown == null) return undefined;
        const next = countdown === 3 ? 2 : countdown === 2 ? 1 : countdown === 1 ? 'go' : null;
        const id = setTimeout(() => setCountdown(next), countdown === 'go' ? 650 : 600);
        return () => clearTimeout(id);
    }, [countdown]);

    // Shared turn resolution: apply the engine result, play the hit beat, then advance or end.
    // Used by both a tapped answer AND a monster time-out (which is just a forced wrong answer).
    const resolveTurn = useCallback((next, fb) => {
        setLocked(true);
        setFeedback(fb);
        setBs(next);
        const beat = next.outcome === 'win' ? 1300 : 950; // longer final blow for the explosion
        setTimeout(() => {
            setFeedback(null);
            setLocked(false);
            if (next.outcome) {
                setPhase('result');
                finishGalaxyBattle(battle.subSubjectId, battle.level, next.outcome === 'win').catch(() => {});
            } else {
                setQIndex(i => i + 1);
            }
        }, beat);
    }, [battle]);

    const handleAnswer = useCallback((picked) => {
        if (locked || countdown != null || !battle || !bs || bs.outcome) return;
        const q = battle.questions[qIndex];
        const isCorrect = String(picked).trim() === String(q?.correctAnswer ?? '').trim();
        const next = applyAnswer(bs, isCorrect);
        resolveTurn(next, { type: isCorrect ? 'correct' : 'wrong', picked, super: next.superUsed });
    }, [locked, countdown, battle, bs, qIndex, resolveTurn]);

    // Monster time-out = a wrong answer with no tapped option: lose a life, reset streak, NO monster
    // HP loss (applyAnswer(false) already does exactly that).
    const handleTimeout = useCallback(() => {
        if (locked || countdown != null || !battle || !bs || bs.outcome) return;
        resolveTurn(applyAnswer(bs, false), { type: 'wrong', timeout: true });
    }, [locked, countdown, battle, bs, resolveTurn]);

    // Monster Charge Timer — only while the question is actually answerable. After a calm grace
    // period the monster "charges" (10→0); reaching 0 fires handleTimeout. The effect owns the
    // timers and tears them down on answer / next question / win / lose / unmount → never stacked.
    const handleTimeoutRef = useRef(null);
    useEffect(() => { handleTimeoutRef.current = handleTimeout; }, [handleTimeout]);
    const answerable = phase === 'battle' && Boolean(battle) && Boolean(bs)
        && countdown == null && !locked && !feedback && !(bs && bs.outcome);

    useEffect(() => {
        if (!answerable) return undefined;
        let tickId;
        const graceId = setTimeout(() => {
            let n = MONSTER_WARNING_SECONDS;
            setMonsterCharge(n);
            tickId = setInterval(() => {
                n -= 1;
                if (n <= 0) {
                    clearInterval(tickId);
                    setMonsterCharge(0);
                    if (handleTimeoutRef.current) handleTimeoutRef.current();
                } else {
                    setMonsterCharge(n);
                }
            }, 1000);
        }, ANSWER_GRACE_SECONDS * 1000);
        // teardown on answer / next question / win / lose / unmount → also hides the warning.
        return () => { clearTimeout(graceId); clearInterval(tickId); setMonsterCharge(null); };
    }, [answerable, qIndex]);

    // The battle is always a dark arcade zone. Everything else — including planet select — follows
    // the user's theme (light/dark) like the rest of the app.
    const inBattle = phase === 'battle' && battle && bs;
    const rootTheme = inBattle ? 'dark' : theme;
    const rootClass = inBattle ? ' gb-root--battle' : phase === 'select' ? ' gb-root--select' : '';

    return (
        <div className={'mg-space gb-root' + rootClass} data-theme={rootTheme} dir={dir}>
            <Stars />
            <div className="sc-content gb-topbar">
                <AppTopBar onBack={() => navigate('/games')} />
            </div>

            {inBattle ? (
                <div className="gb-battle-wrap">
                    <BattleArena
                        question={battle.questions[qIndex]}
                        topicName={battle.topicName}
                        bs={bs}
                        feedback={feedback}
                        locked={locked}
                        countdown={countdown}
                        monsterCharge={monsterCharge}
                        onAnswer={handleAnswer}
                        stars={starsAfter}
                        t={t}
                    />
                </div>
            ) : phase === 'select' ? (
                <>
                    <div className="gb-select-bg" aria-hidden="true">
                        <span className="gb-planet gb-planet--saturn" />
                        <span className="gb-planet gb-planet--blue" />
                        <span className="gb-planet gb-planet--red" />
                    </div>
                    <div className="gb-select-wrap">
                        <TopicPlanetSelector
                            topics={topics}
                            recommendedId={recommendedId}
                            onSelect={startBattleWithTopic}
                            loading={starting}
                            t={t}
                            g={g}
                        />
                    </div>
                </>
            ) : (
                <div className="sc-content gb-inner">
                    {phase === 'loading' && <p className="gb-msg">{t.loading}</p>}
                    {error && <p className="gb-msg gb-msg--err">{error}</p>}

                    {phase === 'result' && bs && (
                        <BattleResult
                            won={bs.outcome === 'win'}
                            topicName={battle?.topicName}
                            bs={bs}
                            onAnotherBattle={() => { setBattle(null); setBs(null); setPhase('select'); }}
                            onBackToGames={() => navigate('/games')}
                            onBackHome={() => navigate('/home')}
                            onPractice={() => navigate('/math-training')}
                            t={t}
                            g={g}
                        />
                    )}
                </div>
            )}

            {notEnough && (
                <div className="gb-overlay" role="dialog" aria-label={t.notEnoughTitle}>
                    <div className="sc-card gb-modal">
                        <div className="gb-modal-emoji" aria-hidden="true">⭐</div>
                        <h2 className="gb-modal-title">{t.notEnoughTitle}</h2>
                        <p className="gb-modal-sub">{g(t.notEnoughBody)}</p>
                        <div className="gb-modal-actions">
                            <button type="button" className="sc-btn" onClick={() => navigate('/math-training')}>{t.goPractice}</button>
                            <button type="button" className="sc-btn sc-btn--ghost" onClick={() => setNotEnough(false)}>{t.close}</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default GalaxyBattleGame;
