import { useEffect, useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useProfile } from '../../contexts/useProfile.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getDashboardData } from '../../service/dashboardApi.js';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import { getGamesStrings } from './gamesStrings.js';
import { GAME_ENTRY_COST } from '../../utils/gameConstants.js';
import { BattlePreviewCard } from './BattlePreviewCard.jsx';
import '../../styles/spaceTokens.css';
import './games.css';

// Student games hub — an immersive galaxy entry page. The main game card carries a live-looking
// preview of the real battle so it "sells" the game. UI only; navigation/logic unchanged.
export const GamesPage = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { language, dir, locale } = useLanguage();
    const t = getGamesStrings(language);
    const theme = (profileData.theme || 'LIGHT').toLowerCase(); // follow the user's theme like every page
    const navigate = useNavigate();

    const gk = user?.gender === 'male' ? 'male' : user?.gender === 'female' ? 'female' : 'neutral';
    const g = (o) => (o && typeof o === 'object' ? (o[gk] ?? o.neutral) : o) || '';

    const [stars, setStars] = useState(null);
    const [notEnough, setNotEnough] = useState(false);

    useEffect(() => {
        let active = true;
        getDashboardData()
            .then(r => { if (active) setStars(r.data?.student?.totalStars ?? null); })
            .catch(() => { /* stars badge just hidden */ });
        return () => { active = false; };
    }, []);

    // "Start battle": gate on stars here (nice message if short), then go to planet select. The
    // authoritative 200-star deduction happens once, server-side, when a planet is chosen.
    const startBattleFlow = () => {
        if (stars != null && stars < GAME_ENTRY_COST) { setNotEnough(true); return; }
        navigate('/games/galaxy-battle');
    };

    return (
        <div className="mg-space games-root" data-theme={theme} dir={dir}>
            <Stars />
            <div className="games-bg" aria-hidden="true">
                <span className="games-planet games-planet--saturn" />
                <span className="games-planet games-planet--blue" />
                <span className="games-planet games-planet--red" />
            </div>

            <div className="sc-content games-topbar"><AppTopBar /></div>

            <div className="sc-content games-stagewrap">
                <aside className="gp-aside gp-aside--l">
                    <span className="gp-aside-star">⭐</span>
                    <p className="gp-aside-text">{t.tipPlay}</p>
                </aside>
                <aside className="gp-aside gp-aside--r">
                    <span className="gp-aside-star gp-aside-star--p">★</span>
                    <p className="gp-aside-text">{t.tipStars}</p>
                </aside>

                <div className="games-inner">
                    <header className="games-hero">
                        <h1 className="games-title">🎮 {t.gamesTitle}</h1>
                        <p className="games-sub">{t.gamesSubtitle}</p>
                        {stars != null && (
                            <div className="games-stars">⭐ {format(t.yourStars, { stars: stars.toLocaleString(locale) })}</div>
                        )}
                    </header>

                    <div className="gp-stage">
                        <span className="gp-stage-rings" aria-hidden="true" />
                        <section className="gp-card">
                            <BattlePreviewCard t={t} />
                            <div className="gp-card-info">
                                <h2 className="gp-card-title">🚀 {t.battleTitle}</h2>
                                <p className="gp-card-desc">{t.battleTagline}</p>
                                <div className="gp-card-cost">⭐ {format(t.playCost, { cost: GAME_ENTRY_COST })}</div>
                                <button type="button" className="sc-btn gp-card-cta" onClick={startBattleFlow}>
                                    🚀 {t.startGame}
                                </button>
                            </div>
                        </section>
                    </div>
                </div>
            </div>

            {notEnough && (
                <div className="games-modal-overlay" role="dialog" aria-label={t.notEnoughTitle}>
                    <div className="games-modal">
                        <div className="games-modal-emoji" aria-hidden="true">⭐</div>
                        <h2 className="games-modal-title">{t.notEnoughTitle}</h2>
                        <p className="games-modal-sub">{g(t.notEnoughBody)}</p>
                        <div className="games-modal-actions">
                            <button type="button" className="sc-btn" onClick={() => navigate('/math-training')}>{t.goPractice}</button>
                            <button type="button" className="sc-btn sc-btn--ghost" onClick={() => setNotEnough(false)}>{t.close}</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default GamesPage;
