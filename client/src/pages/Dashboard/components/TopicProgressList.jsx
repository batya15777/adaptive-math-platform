import { BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ResponsiveContainer, LabelList } from 'recharts';
import { Card, COLORS, rateColor } from './DashboardPrimitives.jsx';

// Success rate per topic, drawn as an animated horizontal bar chart.
// Pure: receives the strings dictionary `t`. (Topic names come from the backend.)
export const TopicProgressList = ({ topics, t }) => {
    const data = (topics || [])
        .slice()
        .sort((a, b) => b.successRate - a.successRate)
        .map((topic) => ({
            name: cap(topic.name),
            successRate: topic.successRate,
            level: topic.currentLevel,
            correct: topic.correct,
            attempts: topic.attempts,
        }));

    return (
        <Card title={t.topicProgressTitle}>
            {data.length === 0 ? (
                <p style={{ color: COLORS.muted, fontSize: 14, margin: 0 }}>
                    {t.noPracticeYet}
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
                            formatter={(v, _n, p) => [`${v}%  (${p.payload.correct}/${p.payload.attempts})`, t.successLabel]}
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
