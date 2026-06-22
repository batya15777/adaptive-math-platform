import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useProfile } from '../../contexts/useProfile.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getHomeStrings } from './homeStrings.js';
import { getDashboardData } from '../../service/dashboardApi.js';
import { getTop10 } from '../../service/leaderboardApi.js';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import { HomeHero } from './HomeHero.jsx';
import { bumpDailyStreak, getDailyProgress } from './homeLocal.js';
import '../../styles/spaceTokens.css';
import './home.css';

const DAILY_GOAL = 5;
const MEDALS = ['🥇', '🥈', '🥉'];

// Operation glyph for a sub-subject (best-effort by name).
const opIcon = (name) => {
    const n = (name || '').toLowerCase();
    if (n.includes('add')) return { sym: '+', bg: '#22C55E' };
    if (n.includes('sub')) return { sym: '−', bg: '#7C4DFF' };
    if (n.includes('mult')) return { sym: '×', bg: '#3B82F6' };
    if (n.includes('div')) return { sym: '÷', bg: '#F59E0B' };
    return { sym: '•', bg: '#7C4DFF' };
};

// Personal student dashboard. Reuses getDashboardData + getTop10; streak/daily are
// isolated local fallbacks. Theme + language + gender aware.
export const Home = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { language, dir, locale } = useLanguage();
    const t = getHomeStrings(language);
    const navigate = useNavigate();
    const theme = (profileData.theme || 'LIGHT').toLowerCase();

    const gk = user?.gender === 'male' ? 'male' : user?.gender === 'female' ? 'female' : 'neutral';
    const g = (o) => (o && (o[gk] ?? o.neutral)) || '';
    const displayName = user?.username || user?.fullName || t.student;

    const [data, setData] = useState(null);
    const [board, setBoard] = useState([]);
    const [streak, setStreak] = useState(0);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState('');

    useEffect(() => {
        let active = true;
        getDashboardData()
            .then(res => { if (active) setData(res.data); })
            .catch(() => { if (active) setErr(t.loadError); })
            .finally(() => { if (active) setLoading(false); });
        getTop10()
            .then(res => { if (active) setBoard(res.data?.leaderboard || []); })
            .catch(() => { /* board stays empty */ });
        return () => { active = false; };
    }, [t.loadError]);

    useEffect(() => {
        if (user?.id != null) setStreak(bumpDailyStreak(user.id));
    }, [user?.id]);

    const stars  = data?.student?.totalStars ?? user?.totalStars ?? 0;
    const topics = data?.topics || [];
    const recs   = data?.recommendations || [];

    const continueTopic = [...topics].sort((a, b) => (b.attempts || 0) - (a.attempts || 0))[0] || null;
    const weakest = topics.length ? [...topics].sort((a, b) => (a.successRate || 0) - (b.successRate || 0))[0] : null;
    const recommended = recs.find(r => r.recommendedSubSubjectName) || null;
    const recoName = recommended?.recommendedSubSubjectName || weakest?.name || null;
    const recoId   = recommended?.recommendedSubSubjectId || weakest?.subSubjectId || null;
    const recoMsg  = recommended?.message || g(t.recommendedExplain);

    const myName = user?.fullName || user?.username;
    const myIdx = board.findIndex(e => e.fullName === myName);
    const rank = myIdx >= 0 ? myIdx + 1 : (board.length ? 0 : null); // 0 = outside top-10, null = unknown
    const top3 = board.slice(0, 3);

    const daily = user?.id != null ? getDailyProgress(user.id, DAILY_GOAL) : { done: 0, goal: DAILY_GOAL };
    const dailyPct = Math.round((daily.done / daily.goal) * 100);

    const play = (id) => navigate(id ? `/math-training/${id}/play` : '/math-training');

    return (
        <div className="mg-space home-root" data-theme={theme} dir={dir}>
            <Stars />
            <div className="sc-content home-inner">
                <AppTopBar />

                <HomeHero displayName={displayName} continueTopic={continueTopic} t={t} g={g} onPlay={play} />

                {loading && <div className="home-msg">{t.loading}</div>}
                {err && <div className="home-msg home-msg--err">{err}</div>}

                {!loading && !err && (
                    <>
                        {/* ── Stats: rank / streak / stars ── */}
                        <section className="home-stats">
                            <div className="sc-card home-stat-card">
                                <div className="home-stat-l">
                                    <div className="home-card-h"><span className="ic">🏅</span>{t.rankTitle}</div>
                                    <div className="home-stat-v accent">
                                        {rank ? format(t.rankPlace, { rank }) : (rank === 0 ? t.rankUnranked : '—')}
                                    </div>
                                    <div className="home-stat-sub">{g(t.rankPraise)}</div>
                                </div>
                                <span className="home-stat-ico">🏆</span>
                            </div>

                            <div className="sc-card home-stat-card">
                                <div className="home-stat-l">
                                    <div className="home-card-h"><span className="ic">🔥</span>{t.streakTitle}</div>
                                    <div className="home-stat-v">{streak}</div>
                                    <div className="home-stat-sub">{format(t.streakDays, { count: streak })}</div>
                                </div>
                                <span className="home-stat-ico">🔥</span>
                            </div>

                            <div className="sc-card home-stat-card">
                                <div className="home-stat-l">
                                    <div className="home-card-h"><span className="ic">⭐</span>{t.starsTitle}</div>
                                    <div className="home-stat-v">{stars.toLocaleString(locale)}</div>
                                    <div className="home-stat-sub">{t.starsTitle}</div>
                                </div>
                                <span className="home-stat-ico">⭐</span>
                            </div>
                        </section>

                        {/* ── Actions: daily / recommended / my path ── */}
                        <section className="home-actions">
                            <div className="sc-card home-card">
                                <div className="home-card-h"><span className="ic">🎯</span>{t.dailyTitle}</div>
                                <div className="home-action-body">
                                    <span className="home-action-sub">{g(t.dailyMotivation)}</span>
                                    <div className="home-bar"><i style={{ width: `${dailyPct}%` }} /></div>
                                    <span className="home-daily-count" dir="ltr">{format(t.dailyGoal, { done: daily.done, goal: daily.goal })}</span>
                                    <button type="button" className="sc-btn" onClick={() => navigate('/daily-practice')}>
                                        {daily.done >= daily.goal ? t.dailyDoneBtn : g(t.startBtn)}
                                    </button>
                                </div>
                            </div>

                            <div className="sc-card home-card home-rec">
                                <div className="home-card-h"><span className="ic">✨</span>{t.recommendedTitle}</div>
                                <div className="home-action-body">
                                    <span className="home-action-sub">{recoMsg}</span>
                                    {recoName && (
                                        <div className="home-rec-box">
                                            <div>
                                                <div className="rb-l">{t.subBoxLabel}</div>
                                                <div className="rb-topic">{recoName}</div>
                                            </div>
                                            <span aria-hidden="true">🎯</span>
                                        </div>
                                    )}
                                    <button type="button" className="sc-btn sc-btn--green" onClick={() => play(recoId)}>{g(t.practiceNowBtn)}</button>
                                </div>
                            </div>

                            <div className="sc-card home-card">
                                <div className="home-card-h"><span className="ic">🛰️</span>{t.pathTitle}</div>
                                <div className="home-action-sub" style={{ marginBottom: 12 }}>{t.pathProgressLabel}</div>
                                {topics.length === 0 ? (
                                    <p className="home-action-sub">{t.pathEmpty}</p>
                                ) : (
                                    <>
                                        <div className="home-chips">
                                            {topics.slice(0, 4).map(tp => {
                                                const op = opIcon(tp.name);
                                                return (
                                                    <div className="home-chip" key={tp.subSubjectId}>
                                                        <span className="chip-ico" style={{ background: op.bg }}>{op.sym}</span>
                                                        <span className="chip-name">{tp.name}</span>
                                                        <span className="chip-pct" dir="ltr">{tp.successRate ?? 0}%</span>
                                                        <span className="chip-bar"><i style={{ width: `${tp.successRate ?? 0}%` }} /></span>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                        <button type="button" className="sc-btn sc-btn--ghost home-allbtn" onClick={() => navigate('/math-training')}>{t.allTopics}</button>
                                    </>
                                )}
                            </div>
                        </section>

                        {/* ── Leaderboard teaser (top-3 podium) ── */}
                        {top3.length > 0 && (
                            <section className="sc-card home-teaser">
                                <div className="home-teaser-cta">
                                    <div className="ht-title"><span aria-hidden="true">🏆</span>{t.teaserTitle}</div>
                                    <div className="ht-sub">{g(t.teaserSub)}</div>
                                    <button type="button" className="home-link" onClick={() => navigate('/leaderboard')}>{t.viewLeaderboard}</button>
                                </div>
                                <div className="home-podium">
                                    {top3.map((e, i) => (
                                        <div className="podium-item" key={i}>
                                            <span className="podium-ava"><span className="podium-medal">{MEDALS[i]}</span></span>
                                            <span className="podium-name">{e.fullName}</span>
                                            <span className="podium-stars">⭐ {(e.totalStars ?? 0).toLocaleString(locale)}</span>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};
