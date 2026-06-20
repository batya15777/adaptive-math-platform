import { BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ResponsiveContainer, LabelList } from 'recharts';
import { Card, COLORS, rateColor } from './DashboardPrimitives.jsx';

// Success rate per topic, drawn as an animated horizontal bar chart.
export const TopicProgressList = ({ topics }) => {
    const data = (topics || [])
        .slice()
        .sort((a, b) => b.successRate - a.successRate)
        .map((t) => ({
            name: cap(t.name),
            successRate: t.successRate,
            level: t.currentLevel,
            correct: t.correct,
            attempts: t.attempts,
        }));

    return (
        <Card title="Progress by topic 📚">
            {data.length === 0 ? (
                <p style={{ color: COLORS.muted, fontSize: 14, margin: 0 }}>
                    No practice yet — head to Math Training to get started.
                </p>
            ) : (
                <ResponsiveContainer width="100%" height={Math.max(140, data.length * 52)}>
                    <BarChart data={data} layout="vertical" margin={{ top: 4, right: 44, bottom: 4, left: 8 }} barCategoryGap={14}>
                        <XAxis type="number" domain={[0, 100]} hide />
                        <YAxis
                            type="category" dataKey="name" width={96}
                            tick={{ fontSize: 13, fill: '#444' }} axisLine={false} tickLine={false}
                        />
                        <Tooltip
                            cursor={{ fill: 'rgba(0,0,0,0.04)' }}
                            formatter={(v, _n, p) => [`${v}%  (${p.payload.correct}/${p.payload.attempts})`, 'Success']}
                            labelFormatter={(l) => l}
                        />
                        <Bar dataKey="successRate" radius={[0, 8, 8, 0]} isAnimationActive animationDuration={900}>
                            {data.map((d, i) => <Cell key={i} fill={rateColor(d.successRate)} />)}
                            <LabelList dataKey="successRate" position="right" formatter={(v) => `${v}%`} style={{ fontSize: 12, fill: '#666', fontWeight: 600 }} />
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            )}
        </Card>
    );
};

const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);
