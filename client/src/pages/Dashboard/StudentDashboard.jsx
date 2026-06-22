import { useContext, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useProfile } from '../../contexts/useProfile.js';
import { getDashboardData } from '../../service/dashboardApi.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { getDashboardStrings } from './dashboardStrings.js';
import { containerVariants } from './components/DashboardPrimitives.jsx';
import { OverviewCards } from './components/OverviewCards.jsx';
import { ClusterCard } from './components/ClusterCard.jsx';
import { TopicProgressList } from './components/TopicProgressList.jsx';
import { SkillRadar } from './components/SkillRadar.jsx';
import { BadgeShelf } from './components/BadgeShelf.jsx';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import '../../styles/spaceTokens.css';
import './StudentDashboard.css';

/**
 * Student Statistics — an analysis/progress view (NOT an action dashboard like Home).
 * Owns data-fetching + loading/error, then composes modular themed sections. Each
 * section is a pure presentational component receiving just its slice of data.
 * Themed via the shared --mg-* tokens (light/dark), gender + language aware.
 */
export const StudentDashboard = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { language, dir, locale } = useLanguage();
    const t = getDashboardStrings(language);
    const theme = (profileData.theme || 'LIGHT').toLowerCase();

    const gk = user?.gender === 'male' ? 'male' : user?.gender === 'female' ? 'female' : 'neutral';
    const g = (o) => (o && (o[gk] ?? o.neutral)) || '';

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        let active = true;
        (async () => {
            setLoading(true);
            setError('');
            try {
                const res = await getDashboardData();
                if (active) setData(res.data);
            } catch {
                if (active) setError(t.loadError);
            } finally {
                if (active) setLoading(false);
            }
        })();
        return () => { active = false; };
    }, [t.loadError]);

    const topics = data?.topics || [];

    return (
        <div className="mg-space stat-root" data-theme={theme} dir={dir}>
            <Stars />
            {/* Wide top bar: logo far right, controls (theme/language/logout) far left. */}
            <div className="sc-content stat-topbar"><AppTopBar /></div>

            <div className="sc-content stat-inner">
                <header className="stat-head">
                    <h1 className="stat-title">{t.statsTitle}</h1>
                    <p className="stat-sub">{g(t.subtitle)}</p>
                </header>

                {loading && <p className="stat-msg">{t.loading}</p>}
                {error && <p className="stat-msg stat-msg--err">{error}</p>}

                {!loading && !error && data && (
                    <motion.div className="stat-sections" variants={containerVariants} initial="hidden" animate="show">
                        <OverviewCards overall={data.overall} t={t} locale={locale} />

                        <TopicProgressList topics={topics} t={t} />

                        {topics.length >= 3 ? (
                            <div className="stat-two">
                                <ClusterCard cluster={data.cluster} t={t} />
                                <SkillRadar topics={topics} t={t} theme={theme} />
                            </div>
                        ) : (
                            <ClusterCard cluster={data.cluster} t={t} />
                        )}

                        <BadgeShelf badges={data.badges} t={t} gk={gk} />
                    </motion.div>
                )}
            </div>
        </div>
    );
};
