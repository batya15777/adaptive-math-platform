import { motion } from 'framer-motion';
import { Card, GaugeChart, COLORS, prettifyError, prettifyClusterLabel, rateColor } from './DashboardPrimitives.jsx';

// "Clustering Insights" — the student's ML cohort, with the group score gauge
// and animated focus-area chips. Pure: receives the strings dictionary `t`.
export const ClusterCard = ({ cluster, t }) => {
    if (!cluster || !cluster.assigned) {
        return (
            <Card title={t.clusterTitle}>
                <p style={{ color: COLORS.muted, margin: 0, fontSize: 14 }}>
                    {t.clusterPending}
                </p>
            </Card>
        );
    }

    const accuracyPct = cluster.avgAccuracy != null ? Math.round(cluster.avgAccuracy * 100) : null;
    const patterns = cluster.topErrorPatterns || [];

    return (
        <Card title={t.clusterTitle}>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
                {accuracyPct != null && (
                    <GaugeChart percent={accuracyPct} color={rateColor(accuracyPct)} size={120} label={t.ourScore} />
                )}
                <div style={{ flex: 1, minWidth: 180 }}>
                    <div style={{ fontSize: 12, color: COLORS.muted, marginBottom: 4 }}>{t.youreInTheGroup}</div>
                    <motion.span
                        style={badge}
                        initial={{ scale: 0.8, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        transition={{ type: 'spring', stiffness: 300, damping: 18 }}
                    >
                        {prettifyClusterLabel(cluster.label, t)}
                    </motion.span>
                    <p style={{ fontSize: 14, color: '#444', margin: '10px 0 0' }}>
                        {t.perfectQuestions}
                    </p>
                </div>
            </div>

            {patterns.length > 0 && (
                <div style={{ marginTop: 14 }}>
                    <div style={{ fontSize: 12, color: COLORS.muted, marginBottom: 6 }}>{t.practicingTogether}</div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                        {patterns.map((p, i) => (
                            <motion.span
                                key={p}
                                style={chip}
                                initial={{ opacity: 0, y: 8 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: 0.2 + i * 0.1 }}
                            >
                                {prettifyError(p, t)}
                            </motion.span>
                        ))}
                    </div>
                </div>
            )}
        </Card>
    );
};

const badge = {
    display: 'inline-block', backgroundColor: '#e7f1ff', color: COLORS.primary, fontWeight: 'bold',
    padding: '6px 14px', borderRadius: 20, fontSize: 14,
};
const chip = {
    backgroundColor: '#fff4e6', color: '#b25e00', border: '1px solid #ffe0b8',
    padding: '4px 10px', borderRadius: 14, fontSize: 12,
};
