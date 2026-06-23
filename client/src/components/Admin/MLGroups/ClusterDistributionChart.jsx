import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from "recharts";
import { Card, COLORS } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import { clusterColor, clusterName, tooltipContentStyle } from "./mlVisuals.js";

// Donut showing how students are distributed across clusters, with the cohort
// total in the hole. Pure: receives `groups`, `t`, `locale`.
export const ClusterDistributionChart = ({ groups, t }) => {
    const data = (groups || []).map((g, i) => {
        const fullName = clusterName(g.label, `#${g.clusterId}`);
        return {
            name: fullName.length > 18 ? fullName.slice(0, 17) + "…" : fullName,
            fullName,
            value: g.memberCount,
            color: clusterColor(i),
        };
    });
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
                                layout="horizontal"
                                iconType="circle"
                                iconSize={9}
                                wrapperStyle={{
                                    paddingTop: 10,
                                    display: "flex",
                                    flexWrap: "wrap",
                                    justifyContent: "center",
                                    gap: "4px 14px",
                                    lineHeight: "1.6",
                                }}
                                formatter={(value) => (
                                    <span style={{
                                        color: "var(--adm-txt-2, #555)",
                                        fontSize: 11,
                                        whiteSpace: "nowrap",
                                        maxWidth: 120,
                                        overflow: "hidden",
                                        textOverflow: "ellipsis",
                                        display: "inline-block",
                                        verticalAlign: "middle",
                                    }}>{value}</span>
                                )}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                    {/* Total in the donut hole */}
                    <div style={center}>
                        <span style={{ fontSize: 30, fontWeight: 800, color: "var(--adm-txt, #222)", lineHeight: 1 }}>{total}</span>
                        <span style={{ fontSize: 11, color: "var(--adm-txt-muted)" }}>{t.membersUnit}</span>
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
    const displayName = p.payload?.fullName || p.name;
    return (
        <div style={{ ...tooltipContentStyle, background: "var(--adm-surface, #fff)", border: "1px solid var(--adm-bd)", color: "var(--adm-txt)" }}>
            <div style={{ fontWeight: 700, marginBottom: 2 }}>{displayName}</div>
            <div style={{ color: "var(--adm-txt-muted)", fontSize: 12 }}>{p.value} {t.membersUnit} · {pct}%</div>
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
