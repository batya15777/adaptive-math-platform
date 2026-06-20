import { RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { Card, COLORS } from './DashboardPrimitives.jsx';

// A "skill map" — success rate across topics as a radar/spider chart.
// Only meaningful with a few topics, so the container renders it conditionally.
export const SkillRadar = ({ topics }) => {
    const data = (topics || []).map((t) => ({
        topic: cap(t.name),
        rate: t.successRate,
    }));

    return (
        <Card title="Your skill map 🕸️">
            <ResponsiveContainer width="100%" height={280}>
                <RadarChart data={data} outerRadius="72%">
                    <PolarGrid stroke="#e6e8ee" />
                    <PolarAngleAxis dataKey="topic" tick={{ fontSize: 12, fill: '#555' }} />
                    <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
                    <Radar
                        dataKey="rate" stroke={COLORS.primary} fill={COLORS.primary} fillOpacity={0.35}
                        isAnimationActive animationDuration={900}
                    />
                    <Tooltip formatter={(v) => [`${v}%`, 'Success']} />
                </RadarChart>
            </ResponsiveContainer>
        </Card>
    );
};

const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);
