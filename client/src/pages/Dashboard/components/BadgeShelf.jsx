import { motion } from 'framer-motion';
import { Card, COLORS } from './DashboardPrimitives.jsx';

// Gamification: earned badges pop in with a spring; locked ones are greyed out.
// Pure: receives the strings dictionary `t`. (Badge name/description come from
// the backend and are rendered as-is.)
export const BadgeShelf = ({ badges, t }) => {
    const items = badges || [];
    const earnedCount = items.filter((b) => b.earned).length;

    return (
        <Card title={`${t.badgesTitle} (${earnedCount}/${items.length})`}>
            <div style={grid}>
                {items.map((b, i) => (
                    <motion.div
                        key={b.id}
                        style={{ ...tile, ...(b.earned ? earnedTile : lockedTile) }}
                        title={b.description}
                        initial={{ opacity: 0, scale: 0.6 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.1 + i * 0.07, type: 'spring', stiffness: 320, damping: 18 }}
                        whileHover={{ scale: 1.06, rotate: b.earned ? -2 : 0 }}
                    >
                        <div style={{ fontSize: 30, filter: b.earned ? 'none' : 'grayscale(1)', opacity: b.earned ? 1 : 0.45 }}>
                            {b.icon}
                        </div>
                        <div style={{ fontSize: 13, fontWeight: 'bold', color: b.earned ? '#333' : COLORS.muted }}>{b.name}</div>
                        <div style={{ fontSize: 11, color: COLORS.muted, textAlign: 'center' }}>{b.description}</div>
                        {b.earned && <span style={earnedTag}>{t.earned}</span>}
                    </motion.div>
                ))}
            </div>
        </Card>
    );
};

const grid = { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 12 };
const tile = {
    position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
    padding: '16px 10px', borderRadius: 14, border: '1px solid #eee',
};
const earnedTile = { backgroundColor: '#f3fbf5', borderColor: '#bfe6c8' };
const lockedTile = { backgroundColor: '#fafafa' };
const earnedTag = {
    position: 'absolute', top: 6, right: 6, fontSize: 10, fontWeight: 'bold',
    color: '#fff', backgroundColor: COLORS.success, borderRadius: 8, padding: '1px 6px',
};
