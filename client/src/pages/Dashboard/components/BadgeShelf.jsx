import { motion } from 'framer-motion';
import { Card } from './DashboardPrimitives.jsx';
import { badgeName } from '../dashboardI18n.js';

// Achievements: earned badges glow in; locked ones are greyed out. Display names are
// localized + gender-aware (the backend sends English). The backend description is NOT
// shown — it's English-only — so the screen stays fully localized.
// Pure: receives the strings dictionary `t` and the gender key `gk`.
export const BadgeShelf = ({ badges, t, gk = 'neutral' }) => {
    const items = badges || [];
    const earnedCount = items.filter((b) => b.earned).length;

    return (
        <Card title={`${t.badgesTitle} (${earnedCount}/${items.length})`}>
            <div className="stat-badges">
                {items.map((b, i) => (
                    <motion.div
                        key={b.id}
                        className={`stat-badge ${b.earned ? 'is-earned' : 'is-locked'}`}
                        initial={{ opacity: 0, scale: 0.7 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.08 + i * 0.06, type: 'spring', stiffness: 320, damping: 18 }}
                        whileHover={{ scale: 1.05, rotate: b.earned ? -2 : 0 }}
                    >
                        <div className="stat-badge-ico">{b.icon}</div>
                        <div className="stat-badge-name">{badgeName(b, t, gk)}</div>
                        {b.earned && <span className="stat-badge-tag">{t.earned}</span>}
                    </motion.div>
                ))}
            </div>
        </Card>
    );
};
