import { Card, GaugeChart, AnimatedNumber, rateColor } from './DashboardPrimitives.jsx';

// Four headline metrics (the mockup's top row): a success-rate gauge plus
// questions-answered, correct-answers and highest-level tiles. No "stars" tile here —
// stars live on Home (the action dashboard); Statistics is analysis only.
// Pure: receives the strings dictionary `t` and the active `locale`.
export const OverviewCards = ({ overall, t, locale }) => {
    const o = overall || {};
    const fmt = (v) => Math.round(v || 0).toLocaleString(locale);
    const metrics = [
        { icon: '🧮', num: o.totalAttempts ?? 0, label: t.questionsAnswered, sub: t.soFar },
        { icon: '✅', num: o.totalCorrect ?? 0,  label: t.correctAnswers,    sub: t.soFar },
        { icon: '🏆', text: `${t.levelShort} ${o.masteryLevel ?? 1}`, label: t.topLevelReached, sub: null },
    ];

    return (
        <div className="stat-overview">
            <Card style={{ padding: 0 }}>
                <div className="stat-gauge-wrap">
                    <GaugeChart percent={o.successRate ?? 0} color={rateColor(o.successRate ?? 0)} label={t.gotItRight} />
                </div>
            </Card>

            {metrics.map((m) => (
                <Card key={m.label} style={{ padding: 0 }}>
                    <div className="stat-metric">
                        <div className="stat-metric-ico">{m.icon}</div>
                        <div className="stat-metric-val">
                            {m.num != null ? <AnimatedNumber value={m.num} format={fmt} /> : m.text}
                        </div>
                        <div className="stat-metric-label">{m.label}</div>
                        {m.sub && <div className="stat-metric-sub">{m.sub}</div>}
                    </div>
                </Card>
            ))}
        </div>
    );
};
