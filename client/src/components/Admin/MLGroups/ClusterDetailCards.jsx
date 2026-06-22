import { motion } from "framer-motion";
import { Card, GaugeChart, COLORS, rateColor } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import { format } from "../../../i18n/languages.js";
import { clusterColor, clusterName, accuracyPct, humanizeError } from "./mlVisuals.js";

// Rich per-cluster cards: an accuracy gauge, the group's share of all students,
// and its focus-area chips. The visual heart of the dashboard. Pure: `groups`, `t`, `locale`.
export const ClusterDetailCards = ({ groups, t, locale }) => {
    const total = (groups || []).reduce((sum, g) => sum + (g.memberCount || 0), 0);

    return (
        <div style={grid}>
            {(groups || []).map((g, i) => {
                const color = clusterColor(i);
                const pct = accuracyPct(g.avgAccuracy);
                const share = total > 0 ? Math.round((g.memberCount / total) * 100) : 0;
                const focus = g.topErrors || [];
                return (
                    <Card key={g.clusterId ?? i} style={{ padding: 0, overflow: "hidden" }}>
                        <div style={{ height: 6, background: color }} />
                        <div style={{ padding: 18 }}>
                            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12 }}>
                                <span style={{ width: 10, height: 10, borderRadius: "50%", background: color, flexShrink: 0 }} />
                                <h3 style={{ margin: 0, fontSize: 15.5, color: "#2b2b35", flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                    {clusterName(g.label, `#${g.clusterId}`)}
                                </h3>
                                <span style={idBadge}>#{g.clusterId}</span>
                            </div>

                            <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
                                <GaugeChart
                                    percent={pct ?? 0}
                                    color={pct == null ? COLORS.track : rateColor(pct)}
                                    size={104}
                                    label={t.statAvgAccuracy}
                                />
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div style={{ fontSize: 26, fontWeight: 800, color: "#222", lineHeight: 1 }}>
                                        {g.memberCount}
                                    </div>
                                    <div style={{ fontSize: 12, color: COLORS.muted }}>{t.membersUnit}</div>
                                    <ShareBar pct={share} color={color} />
                                    <div style={{ fontSize: 11, color: "#aaa", marginTop: 4 }}>
                                        {format(t.ofTotalShare, { percent: share })}
                                    </div>
                                </div>
                            </div>

                            <div style={{ marginTop: 14 }}>
                                <div style={{ fontSize: 11.5, color: COLORS.muted, marginBottom: 6 }}>{t.focusAreas}</div>
                                {focus.length > 0 ? (
                                    <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                                        {focus.map((code, ci) => (
                                            <motion.span
                                                key={code}
                                                style={chip}
                                                initial={{ opacity: 0, y: 6 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                transition={{ delay: 0.15 + ci * 0.07 }}
                                            >
                                                {humanizeError(code, t)}
                                            </motion.span>
                                        ))}
                                    </div>
                                ) : (
                                    <span style={{ fontSize: 13, color: COLORS.success }}>{t.noFocusAreas}</span>
                                )}
                            </div>

                            {g.lastAssignedAt && (
                                <div style={{ fontSize: 11, color: "#b3b3bd", marginTop: 12 }}>
                                    {t.statLastRun}: {new Date(g.lastAssignedAt).toLocaleString(locale)}
                                </div>
                            )}
                        </div>
                    </Card>
                );
            })}
        </div>
    );
};

const ShareBar = ({ pct, color }) => (
    <div style={{ height: 8, background: COLORS.track, borderRadius: 6, overflow: "hidden", marginTop: 8 }}>
        <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${pct}%` }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            style={{ height: "100%", background: color, borderRadius: 6 }}
        />
    </div>
);

const grid = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 16 };
const idBadge = {
    fontSize: 11, fontWeight: 700, color: "#8a8a96", background: "#f1f2f6",
    borderRadius: 8, padding: "2px 8px", flexShrink: 0,
};
const chip = {
    backgroundColor: "#fff4e6", color: "#b25e00", border: "1px solid #ffe0b8",
    padding: "4px 10px", borderRadius: 14, fontSize: 12,
};
