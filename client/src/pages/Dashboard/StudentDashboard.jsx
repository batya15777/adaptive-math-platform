import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { getDashboardData } from '../../service/dashboardApi.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getDashboardStrings } from './dashboardStrings.js';
import { containerVariants, itemVariants } from './components/DashboardPrimitives.jsx';
import { OverviewCards } from './components/OverviewCards.jsx';
import { ClusterCard } from './components/ClusterCard.jsx';
import { AdaptiveAlerts } from './components/AdaptiveAlerts.jsx';
import { TopicProgressList } from './components/TopicProgressList.jsx';
import { SkillRadar } from './components/SkillRadar.jsx';
import { BadgeShelf } from './components/BadgeShelf.jsx';

/**
 * Container for the Student Learning Dashboard. Owns data-fetching + loading/error,
 * then composes modular, animated sections (recharts visuals + framer-motion).
 * Each section is a pure presentational component receiving just its slice of data.
 */
// Renders under DashboardLayout, which owns `dir` — so no dir on this root.
export const StudentDashboard = () => {
    const { language, locale } = useLanguage();
    const t = getDashboardStrings(language);

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

    if (loading) return <div style={page}><p style={{ color: '#888' }}>{t.loading}</p></div>;
    if (error)   return <div style={page}><p style={{ color: '#dc3545' }}>{error}</p></div>;
    if (!data)   return null;

    const topics = data.topics || [];

    return (
        <motion.div style={page} variants={containerVariants} initial="hidden" animate="show">
            <motion.header variants={itemVariants} style={{ marginBottom: 2 }}>
                <h1 style={heading}>{format(t.greeting, { name: data.student?.name || t.greetingFallbackName })}</h1>
                <p style={sub}>{t.subtitle}</p>
            </motion.header>

            <OverviewCards overall={data.overall} totalStars={data.student?.totalStars} t={t} locale={locale} />

            <div style={twoCol}>
                <ClusterCard cluster={data.cluster} t={t} />
                <AdaptiveAlerts recommendations={data.recommendations} t={t} />
            </div>

            {topics.length >= 3 ? (
                <div style={twoCol}>
                    <TopicProgressList topics={topics} t={t} />
                    <SkillRadar topics={topics} t={t} />
                </div>
            ) : (
                <TopicProgressList topics={topics} t={t} />
            )}

            <BadgeShelf badges={data.badges} t={t} />
        </motion.div>
    );
};

// ── styles ──────────────────────────────────────────────────────────────────
const page = {
    padding: '24px', maxWidth: '960px', margin: '0 auto',
    fontFamily: 'Arial, sans-serif', display: 'flex', flexDirection: 'column', gap: 18,
};
const heading = { margin: '0 0 4px', fontSize: 26, color: '#222' };
const sub = { margin: 0, fontSize: 14, color: '#888' };
const twoCol = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 18 };
