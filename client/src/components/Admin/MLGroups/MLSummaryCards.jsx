import { motion } from "framer-motion";
import {
    Card, GaugeChart, AnimatedNumber, COLORS, rateColor,
} from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import {
    clusterName, accuracyPct, overallAvgAccuracy, largestGroup, aggregateErrors,
} from "./mlVisuals.js";

// Headline KPIs for the clustering run: a member-weighted accuracy gauge plus
// four count-up tiles. Mirrors the Student Dashboard's OverviewCards so the two
// dashboards feel like one product. Pure: receives `data`, `t`, `locale`.
export const MLSummaryCards = ({ data, t, locale }) => {
    const groups = data.groups || [];
    const fmtNum = (v) => Math.round(v).toLocaleString(locale);

    const avgPct = accuracyPct(overallAvgAccuracy(groups));
    const biggest = largestGroup(groups);
    const distinctFocus = aggregateErrors(groups, t).length;
    const modelK = groups.find((g) => g.modelK != null)?.modelK ?? groups.length;

    const tiles = [
        { icon: "👥", color: COLORS.primary, value: <AnimatedNumber value={data.totalAssigned || 0} format={fmtNum} />, label: t.statStudents },
        { icon: "🧩", color: COLORS.purple, value: <AnimatedNumber value={groups.length} format={fmtNum} />, label: t.statGroups, caption: `${t.modelKLabel}: ${modelK}` },
        {
            icon: "🏆", color: COLORS.warning,
            value: <AnimatedNumber value={biggest ? biggest.memberCount : 0} format={fmtNum} />,
            label: biggest ? clusterName(biggest.label, `#${biggest.clusterId}`) : "—",
            caption: t.statLargestGroup,
        },
        { icon: "🎯", color: COLORS.success, value: <AnimatedNumber value={distinctFocus} format={fmtNum} />, label: t.focusAreas },
    ];

    return (
        <Card title={t.mlOverview}>
            <div style={row}>
                <div style={{ display: "flex", justifyContent: "center", minWidth: 170 }}>
                    <GaugeChart
                        percent={avgPct ?? 0}
                        color={avgPct == null ? COLORS.track : rateColor(avgPct)}
                        label={t.statAvgAccuracy}
                    />
                </div>
                <div style={statsGrid}>
                    {tiles.map((item, i) => (
                        <motion.div
                            key={item.label + i}
                            style={tile}
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            transition={{ delay: 0.12 + i * 0.08, type: "spring", stiffness: 280, damping: 20 }}
                            whileHover={{ scale: 1.04 }}
                        >
                            <div style={{ fontSize: 20 }}>{item.icon}</div>
                            <div style={{ fontSize: 24, fontWeight: "bold", color: item.color }}>{item.value}</div>
                            <div style={{ fontSize: 12, color: COLORS.muted, maxWidth: "100%", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                {item.label}
                            </div>
                            {item.caption && <div style={{ fontSize: 11, color: "#b3b3bd" }}>{item.caption}</div>}
                        </motion.div>
                    ))}
                </div>
            </div>
        </Card>
    );
};

const row = { display: "flex", gap: 24, alignItems: "center", flexWrap: "wrap" };
const statsGrid = { display: "grid", gridTemplateColumns: "repeat(2, minmax(130px, 1fr))", gap: 12, flex: 1 };
const tile = {
    display: "flex", flexDirection: "column", gap: 2, alignItems: "flex-start",
    padding: "12px 14px", backgroundColor: "var(--adm-surface-2, #f8f9fb)",
    borderRadius: 12, border: "1px solid var(--adm-bd, #eee)",
};
