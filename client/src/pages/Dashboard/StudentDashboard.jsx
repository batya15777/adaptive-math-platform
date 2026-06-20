import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { getDashboardData } from '../../service/dashboardApi.js';
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
export const StudentDashboard = () => {
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
                if (active) setError('Could not load your dashboard. Please try again.');
            } finally {
                if (active) setLoading(false);
            }
        })();
        return () => { active = false; };
    }, []);

    if (loading) return <div style={page}><p style={{ color: '#888' }}>Loading your dashboard…</p></div>;
    if (error)   return <div style={page}><p style={{ color: '#dc3545' }}>{error}</p></div>;
    if (!data)   return null;

    const topics = data.topics || [];

    return (
        <motion.div style={page} variants={containerVariants} initial="hidden" animate="show">
            <motion.header variants={itemVariants} style={{ marginBottom: 2 }}>
                <h1 style={heading}>Hi {data.student?.name || 'there'} 👋</h1>
                <p style={sub}>Here's your personalised learning dashboard.</p>
            </motion.header>

            <OverviewCards overall={data.overall} totalStars={data.student?.totalStars} />

            <div style={twoCol}>
                <ClusterCard cluster={data.cluster} />
                <AdaptiveAlerts recommendations={data.recommendations} />
            </div>

            {topics.length >= 3 ? (
                <div style={twoCol}>
                    <TopicProgressList topics={topics} />
                    <SkillRadar topics={topics} />
                </div>
            ) : (
                <TopicProgressList topics={topics} />
            )}

            <BadgeShelf badges={data.badges} />
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
