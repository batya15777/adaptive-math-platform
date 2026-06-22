import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell, LabelList, ResponsiveContainer,
} from "recharts";
import { Card, COLORS, rateColor } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import {
    clusterName, accuracyPct, tooltipContentStyle, tooltipItemStyle, tooltipLabelStyle,
} from "./mlVisuals.js";

// Average accuracy per cluster. Bars are coloured on the shared performance scale
// (green ≥70 / amber ≥40 / red) so weak cohorts pop instantly. Pure: `groups`, `t`.
export const ClusterAccuracyChart = ({ groups, t }) => {
    const data = (groups || []).map((g, i) => {
        const pct = accuracyPct(g.avgAccuracy);
        return {
            name: clusterName(g.label, `#${g.clusterId}`),
            accuracy: pct ?? 0,
            fill: pct == null ? COLORS.track : rateColor(pct),
            id: g.clusterId ?? i,
        };
    });

    return (
        <Card title={t.chartAccuracyTitle}>
            <ResponsiveContainer width="100%" height={280}>
                <BarChart data={data} margin={{ top: 18, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#eef1f5" />
                    <XAxis
                        dataKey="name" interval={0} tickLine={false} axisLine={{ stroke: "#e6e8ee" }}
                        tick={{ fontSize: 11, fill: "#666" }} height={48} angle={data.length > 4 ? -15 : 0} textAnchor={data.length > 4 ? "end" : "middle"}
                    />
                    <YAxis
                        domain={[0, 100]} ticks={[0, 25, 50, 75, 100]} tickFormatter={(v) => `${v}%`}
                        tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: "#999" }}
                    />
                    <Tooltip
                        cursor={{ fill: "rgba(0,0,0,0.04)" }}
                        contentStyle={tooltipContentStyle}
                        itemStyle={tooltipItemStyle}
                        labelStyle={tooltipLabelStyle}
                        formatter={(v) => [`${v}%`, t.statAvgAccuracy]}
                    />
                    <Bar dataKey="accuracy" radius={[6, 6, 0, 0]} maxBarSize={64} isAnimationActive animationDuration={800}>
                        {data.map((d) => <Cell key={d.id} fill={d.fill} />)}
                        <LabelList dataKey="accuracy" position="top" formatter={(v) => `${v}%`} style={{ fontSize: 11, fill: "#555", fontWeight: 600 }} />
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </Card>
    );
};
