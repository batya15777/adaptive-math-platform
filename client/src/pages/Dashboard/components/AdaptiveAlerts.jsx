import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Card, COLORS, severityColor } from './DashboardPrimitives.jsx';

// The "Adaptive Alert" section — dynamically suggests what to practise next,
// driven by the ML cluster's (or the student's own) top error patterns.
// Pure: receives the strings dictionary `t`. Note: rec.title/rec.message are
// produced by the backend and rendered as-is.
export const AdaptiveAlerts = ({ recommendations, t }) => {
    const navigate = useNavigate();
    const items = recommendations || [];

    const practise = (rec) => {
        if (rec.recommendedSubSubjectId) {
            navigate(`/math-training/${rec.recommendedSubSubjectId}/play`, {
                state: { subSubjectName: rec.recommendedSubSubjectName || t.practiceFallback },
            });
        } else {
            navigate('/math-training');
        }
    };

    return (
        <Card title={t.recommendedTitle}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {items.map((rec, i) => {
                    const color = severityColor(rec.severity);
                    return (
                        <motion.div
                            key={i}
                            style={{ ...alert, borderLeft: `4px solid ${color}` }}
                            initial={{ opacity: 0, x: -16 }}
                            animate={{ opacity: 1, x: 0 }}
                            transition={{ delay: 0.15 + i * 0.1, type: 'spring', stiffness: 260, damping: 22 }}
                        >
                            <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 'bold', color: '#333', marginBottom: 2 }}>{rec.title}</div>
                                <div style={{ fontSize: 13, color: '#555' }}>{rec.message}</div>
                            </div>
                            <motion.button
                                onClick={() => practise(rec)}
                                style={{ ...practiceBtn, backgroundColor: color }}
                                whileHover={{ scale: 1.05 }}
                                whileTap={{ scale: 0.96 }}
                            >
                                {t.practiceNow}
                            </motion.button>
                        </motion.div>
                    );
                })}
                {items.length === 0 && (
                    <p style={{ color: COLORS.muted, fontSize: 14, margin: 0 }}>{t.noRecommendations}</p>
                )}
            </div>
        </Card>
    );
};

const alert = {
    display: 'flex', alignItems: 'center', gap: 12,
    backgroundColor: '#f8f9fb', borderRadius: 10, padding: '12px 14px',
};
const practiceBtn = {
    color: '#fff', border: 'none', borderRadius: 8, padding: '8px 12px',
    fontSize: 13, fontWeight: 'bold', cursor: 'pointer', whiteSpace: 'nowrap',
};
