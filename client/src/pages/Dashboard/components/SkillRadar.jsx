import { RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { Card } from './DashboardPrimitives.jsx';
import { topicName } from '../dashboardI18n.js';

// "Skill map" — success rate across topics as a translucent purple radar/spider chart.
// Topic labels are localized. Pure: receives the strings dictionary `t` + active `theme`
// (so the grid/axis adapt to light/dark). Only the student's own data is plotted — there
// is no group-average series in the API, so none is invented.
export const SkillRadar = ({ topics, t, theme }) => {
    const data = (topics || []).map((tp) => ({ topic: topicName(tp.name, t), rate: tp.successRate }));
    const grid = theme === 'dark' ? 'rgba(255,255,255,.14)' : '#e6e8ee';
    const axisFill = theme === 'dark' ? '#CBBBFF' : '#555';

    return (
        <Card title={t.skillMapTitle}>
            <p className="stat-radar-sub">{t.skillMapSubtitle}</p>
            <ResponsiveContainer width="100%" height={260}>
                <RadarChart data={data} outerRadius="70%">
                    <PolarGrid stroke={grid} />
                    <PolarAngleAxis dataKey="topic" tick={{ fontSize: 12, fill: axisFill }} />
                    <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
                    <Radar dataKey="rate" stroke="#7C4DFF" fill="#7C4DFF" fillOpacity={0.32} isAnimationActive animationDuration={900} />
                    <Tooltip formatter={(v) => [`${v}%`, t.successLabel]} />
                </RadarChart>
            </ResponsiveContainer>
            <div className="stat-radar-legend"><span><i />{t.skillYou}</span></div>
        </Card>
    );
};
