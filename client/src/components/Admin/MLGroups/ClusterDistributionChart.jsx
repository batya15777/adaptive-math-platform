import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from "recharts";
import { Card, COLORS } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import { clusterColor, clusterName, tooltipContentStyle } from "./mlVisuals.js";

// Donut showing how students are distributed across clusters, with the cohort
// total in the hole. Pure: receives `groups`, `t`, `locale`.
export const ClusterDistributionChart = ({ groups, t }) => {
    const data = (groups || []).map((g, i) => ({
        name: clusterName(g.label, `#${g.clusterId}`),
        value: g.memberCount,
        color: clusterColor(i),
    }));
    const total = data.reduce((sum, d) => sum + (d.value || 0), 0);

    return (
        <Card title={t.chartDistributionTitle}>
            {total === 0 ? (
                <Empty text={t.chartDistributionEmpty} />
            ) : (
                <div style={{ position: "relative" }}>
                    <ResponsiveContainer width="100%" height={280}>
                        <PieChart>
                            <Pie
                                data={data}
                                dataKey="value"
                                nameKey="name"
                                innerRadius="60%"
                                outerRadius="88%"
                                paddingAngle={data.length > 1 ? 2 : 0}
                                stroke="none"
                                isAnimationActive
                                animationDuration={800}
                            >
                                {data.map((d) => <Cell key={d.name} fill={d.color} />)}
                            </Pie>
                            <Tooltip content={<DonutTooltip total={total} t={t} />} />
                            <Legend
                                verticalAlign="bottom"
                                height={28}
                                iconType="circle"
                                formatter={(value) => <span style={{ color: "#555", fontSize: 12 }}>{value}</span>}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                    {/* Total in the donut hole */}
                    <div style={center}>
                        <span style={{ fontSize: 30, fontWeight: 800, color: "#222", lineHeight: 1 }}>{total}</span>
                        <span style={{ fontSize: 11, color: COLORS.muted }}>{t.membersUnit}</span>
                    </div>
                </div>
            )}
        </Card>
    );
};

const DonutTooltip = ({ active, payload, total, t }) => {
    if (!active || !payload || !payload.length) return null;
    const p = payload[0];
    const pct = total > 0 ? Math.round((p.value / total) * 100) : 0;
    return (
        <div style={tooltipContentStyle}>
            <div style={{ fontWeight: 700, color: "#2b2b35", marginBottom: 2 }}>{p.name}</div>
            <div style={{ color: COLORS.muted }}>{p.value} {t.membersUnit} · {pct}%</div>
        </div>
    );
};

const Empty = ({ text }) => (
    <div style={{ height: 280, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.muted, fontSize: 14 }}>
        {text}
    </div>
);

// The donut-hole overlay sits above the chart but must not eat hover events.
const center = {
    position: "absolute", inset: 0, bottom: 28, display: "flex", flexDirection: "column",
    alignItems: "center", justifyContent: "center", pointerEvents: "none",
};
