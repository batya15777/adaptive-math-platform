import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell, LabelList, ResponsiveContainer,
} from "recharts";
import { Card, COLORS } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import { aggregateErrors, tooltipContentStyle, tooltipItemStyle, tooltipLabelStyle } from "./mlVisuals.js";

// Horizontal ranking of the cohort's most common focus areas (error patterns),
// weighted by how many students sit in a group that struggles with each one.
// The top area is highlighted in red, the next in amber. Pure: `groups`, `t`.
export const ErrorPatternsChart = ({ groups, t }) => {
    const data = aggregateErrors(groups || [], t);
    const ramp = [COLORS.error, COLORS.warning, COLORS.primary];
    const barColor = (i) => ramp[i] || COLORS.purple;
    const height = Math.max(180, data.length * 46 + 24);

    return (
        <Card title={t.chartErrorsTitle}>
            {data.length === 0 ? (
                <div style={{ height: 180, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.muted, fontSize: 14 }}>
                    {t.chartErrorsEmpty}
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={height}>
                    <BarChart data={data} layout="vertical" margin={{ top: 0, right: 28, left: 8, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#eef1f5" />
                        <XAxis type="number" allowDecimals={false} tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: "#999" }} />
                        <YAxis
                            type="category" dataKey="name" width={150} tickLine={false}
                            axisLine={{ stroke: "#e6e8ee" }} tick={{ fontSize: 12, fill: "#555" }}
                        />
                        <Tooltip
                            cursor={{ fill: "rgba(0,0,0,0.04)" }}
                            contentStyle={tooltipContentStyle}
                            itemStyle={tooltipItemStyle}
                            labelStyle={tooltipLabelStyle}
                            formatter={(v) => [`${v} ${t.membersUnit}`, t.focusAreas]}
                        />
                        <Bar dataKey="count" radius={[0, 6, 6, 0]} maxBarSize={34} isAnimationActive animationDuration={800}>
                            {data.map((d, i) => <Cell key={d.code} fill={barColor(i)} />)}
                            <LabelList dataKey="count" position="right" style={{ fontSize: 11, fill: "#666", fontWeight: 600 }} />
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            )}
        </Card>
    );
};
