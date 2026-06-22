import { useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Confetti from 'react-confetti';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useProfile } from '../../contexts/useProfile.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getDashboardData } from '../../service/dashboardApi.js';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import { QuestionCard } from '../../components/practice/QuestionCard.jsx';
import { TutorChatLauncher } from '../../components/TutorChat/TutorChatLauncher.jsx';
import { getQuestionGameStrings } from '../QuestionGame/questionGameStrings.js';
import { isDailyComplete } from '../Home/homeLocal.js';
import { useDailySession } from './useDailySession.js';
import { getDailyStrings } from './dailyStrings.js';
import { DAILY_GOAL, DAILY_BONUS, progressPct } from './dailyLogic.js';
import '../../styles/spaceTokens.css';
import './DailyPractice.css';

// Themed page frame shared by every view (loading / question / completion).
// The top bar spans a wide column (same structure as Home — logo far right, controls
// far left) while the practice content stays in a narrow, centred column.
const DailyShell = ({ theme, dir, children }) => (
    <div className="mg-space dp-root" data-theme={theme} dir={dir}>
        <Stars />
        <div className="sc-content dp-topbar"><AppTopBar backTo="/home" /></div>
        <div className="sc-content dp-inner">{children}</div>
    </div>
);

export const DailyPractice = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { language, dir, locale } = useLanguage();
    const navigate = useNavigate();
    const theme = (profileData.theme || 'LIGHT').toLowerCase();

    // Memoized so they keep a stable identity across renders (only changing with
    // language) — otherwise the session's load effect would re-fire every render.
    const dt = useMemo(() => getDailyStrings(language), [language]);
    const qt = useMemo(() => getQuestionGameStrings(language), [language]);
    const gk = user?.gender === 'male' ? 'male' : user?.gender === 'female' ? 'female' : 'neutral';
    const g = (o) => (o && (o[gk] ?? o.neutral)) || '';

    const alreadyDone = user?.id != null && isDailyComplete(user.id, DAILY_GOAL);
    const [topics, setTopics] = useState([]);
    const [bootstrapping, setBootstrapping] = useState(!alreadyDone);
    const [loadError, setLoadError] = useState('');

    useEffect(() => {
        if (alreadyDone) return; // nothing to fetch — show the "already done" view
        let active = true;
        getDashboardData()
            .then(res => { if (active) setTopics(res.data?.topics || []); })
            .catch(() => { if (active) setLoadError(dt.loadError); })
            .finally(() => { if (active) setBootstrapping(false); });
        return () => { active = false; };
    }, [alreadyDone, dt.loadError]);

    const session = useDailySession({ topics, language, qt, userId: user?.id, goal: DAILY_GOAL });

    const view = alreadyDone ? 'already'
        : bootstrapping ? 'loading'
        : loadError ? 'error'
        : topics.length === 0 ? 'empty'
        : session.phase; // loading | answering | concluded | done | error

    const correct = session.correct;
    const q = session.question;
    const fb = session.feedback;
    const stars = session.totalStars;
    const pct = progressPct(correct, DAILY_GOAL);

    // ── completion (celebrate or already-done) ──────────────────────────────────
    if (view === 'done' || view === 'already') {
        return (
            <DailyShell theme={theme} dir={dir}>
                {view === 'done' && (
                    <Confetti width={window.innerWidth} height={window.innerHeight}
                        recycle={false} numberOfPieces={420} gravity={0.22}
                        style={{ position: 'fixed', inset: 0, zIndex: 50, pointerEvents: 'none' }} />
                )}
                <div className="sc-card dp-done">
                    <div className="dp-done-emoji" aria-hidden="true">🎉</div>
                    <h1 className="dp-done-title">{dt.doneTitle}</h1>
                    {view === 'done' ? (
                        <>
                            <p className="dp-done-praise">{g(dt.donePraise)}</p>
                            <div className="dp-done-bonus">{format(dt.doneBonus, { bonus: DAILY_BONUS })}</div>
                            {stars != null && (
                                <p className="dp-done-stars">{format(dt.doneStars, { stars: stars.toLocaleString(locale) })}</p>
                            )}
                        </>
                    ) : (
                        <p className="dp-done-praise">{dt.alreadyDone}</p>
                    )}
                    <button type="button" className="sc-btn dp-done-btn" onClick={() => navigate('/home')}>{dt.backHome}</button>
                </div>
            </DailyShell>
        );
    }

    return (
        <DailyShell theme={theme} dir={dir}>
            <div className="dp-head">
                <h1 className="dp-title">🎯 {dt.title}</h1>
                <p className="dp-sub">{format(dt.subtitle, { goal: DAILY_GOAL })}</p>
            </div>

            <div className="dp-progress">
                <div className="dp-bar"><i style={{ width: `${pct}%` }} /></div>
                <span className="dp-count" dir="ltr">{format(dt.progress, { done: correct, goal: DAILY_GOAL })}</span>
            </div>

            <div className="sc-card dp-card">
                {view === 'loading' && <p className="dp-msg">{dt.loading}</p>}
                {view === 'empty' && <p className="dp-msg">{dt.empty}</p>}
                {view === 'error' && <p className="dp-msg dp-msg--err">{loadError || session.error || dt.loadError}</p>}

                {(view === 'answering' || view === 'concluded') && q && (
                    <>
                        <QuestionCard
                            key={q.questionId}
                            question={q}
                            t={qt}
                            feedback={fb}
                            locked={view !== 'answering' || session.busy}
                            onAnswer={session.answer}
                        />
                        {session.error && <p className="dp-msg dp-msg--err">{session.error}</p>}
                        {view === 'concluded' && (
                            <button type="button" className="sc-btn dp-next" disabled={session.busy} onClick={session.next}>
                                {dt.next}
                            </button>
                        )}
                    </>
                )}
            </div>

            {/* Reuse the existing AI tutor (not a new build) as a floating widget — closed
                by default, opens on click. Keyed by question so it resets each question. */}
            {q && (view === 'answering' || view === 'concluded') && (
                <TutorChatLauncher key={q.questionId} questionId={q.questionId} language={language} />
            )}
        </DailyShell>
    );
};
