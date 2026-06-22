import { Card } from './DashboardPrimitives.jsx';
import { topicName } from '../dashboardI18n.js';

// Per-topic success rate as clean themed bars (purple→blue gradient). Topic names are
// localized (the backend sends English). Pure: receives the strings dictionary `t`.
const ICONS = {
    add: { s: '+', c: '#5B8CFF' }, sub: { s: '−', c: '#7C4DFF' }, mult: { s: '×', c: '#6366F1' },
    div: { s: '÷', c: '#8B5CF6' }, fractions: { s: '½', c: '#A78BFA' }, decimals: { s: '.5', c: '#7C4DFF' },
    default: { s: '•', c: '#7C4DFF' },
};
const iconFor = (name) => {
    const n = (name || '').toLowerCase();
    if (n.includes('add')) return ICONS.add;
    if (n.includes('sub')) return ICONS.sub;
    if (n.includes('mult')) return ICONS.mult;
    if (n.includes('div')) return ICONS.div;
    if (n.includes('frac')) return ICONS.fractions;
    if (n.includes('dec')) return ICONS.decimals;
    return ICONS.default;
};

export const TopicProgressList = ({ topics, t }) => {
    const data = (topics || []).slice().sort((a, b) => (b.successRate || 0) - (a.successRate || 0));

    return (
        <Card title={t.topicProgressTitle}>
            {data.length === 0 ? (
                <p style={{ color: 'var(--mg-tm)', fontSize: 14, margin: 0 }}>{t.noPracticeYet}</p>
            ) : (
                <div className="stat-bars">
                    {data.map((tp) => {
                        const ic = iconFor(tp.name);
                        const pct = Math.max(0, Math.min(100, Math.round(tp.successRate || 0)));
                        return (
                            <div className="stat-bar-row" key={tp.subSubjectId ?? tp.name}>
                                <span className="stat-bar-ico" style={{ background: ic.c }}>{ic.s}</span>
                                <span className="stat-bar-name">{topicName(tp.name, t)}</span>
                                <span className="stat-bar-track"><i className="stat-bar-fill" style={{ width: `${pct}%` }} /></span>
                                <span className="stat-bar-pct" dir="ltr">{pct}%</span>
                            </div>
                        );
                    })}
                </div>
            )}
        </Card>
    );
};
