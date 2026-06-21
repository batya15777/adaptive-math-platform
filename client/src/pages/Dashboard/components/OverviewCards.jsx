import { motion } from 'framer-motion';
import { Card, GaugeChart, AnimatedNumber, rateColor, COLORS } from './DashboardPrimitives.jsx';

// Headline stats: an animated success-rate gauge plus four count-up stat tiles.
// Pure: receives the strings dictionary `t` and the active `locale` via props.
export const OverviewCards = ({ overall, totalStars, t, locale }) => {
    const o = overall || {};
    const fmtNum = (v) => Math.round(v).toLocaleString(locale);
    const tiles = [
        { icon: '⭐', value: totalStars ?? 0,      label: t.stars,             color: COLORS.warning },
        { icon: '🧮', value: o.totalAttempts ?? 0, label: t.questionsAnswered, color: COLORS.primary },
        { icon: '✅', value: o.totalCorrect ?? 0,  label: t.correctAnswers,    color: COLORS.success },
    ];

    return (
        <Card title={t.overviewTitle}>
            <div style={row}>
                <div style={{ display: 'flex', justifyContent: 'center', minWidth: 170 }}>
                    <GaugeChart percent={o.successRate ?? 0} color={rateColor(o.successRate ?? 0)} label={t.gotItRight} />
                </div>
                <div style={statsGrid}>
                    {tiles.map((item, i) => (
                        <motion.div
                            key={item.label}
                            style={tile}
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            transition={{ delay: 0.15 + i * 0.08, type: 'spring', stiffness: 280, damping: 20 }}
                            whileHover={{ scale: 1.04 }}
                        >
                            <div style={{ fontSize: 20 }}>{item.icon}</div>
                            <div style={{ fontSize: 24, fontWeight: 'bold', color: item.color }}>
                                <AnimatedNumber value={item.value} format={fmtNum} />
                            </div>
                            <div style={{ fontSize: 12, color: COLORS.muted }}>{item.label}</div>
                        </motion.div>
                    ))}
                    <motion.div
                        style={{ ...tile, background: 'linear-gradient(135deg,#eef4ff,#e7ecff)' }}
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.15 + 3 * 0.08, type: 'spring', stiffness: 280, damping: 20 }}
                        whileHover={{ scale: 1.04 }}
                    >
                        <div style={{ fontSize: 20 }}>🏆</div>
                        <div style={{ fontSize: 24, fontWeight: 'bold', color: COLORS.purple }}>{t.levelShort} {o.masteryLevel ?? 1}</div>
                        <div style={{ fontSize: 12, color: COLORS.muted }}>{t.topLevelReached}</div>
                    </motion.div>
                </div>
            </div>
        </Card>
    );
};

const row = { display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' };
const statsGrid = { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(120px, 1fr))', gap: 12, flex: 1 };
const tile = {
    display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'flex-start',
    padding: '12px 14px', backgroundColor: '#f8f9fb', borderRadius: 12, border: '1px solid #eee',
};
