import { motion } from "framer-motion";
import { GaugeChart, COLORS, rateColor } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";
import { format } from "../../../i18n/languages.js";
import { clusterColor, clusterName, accuracyPct, humanizeError } from "./mlVisuals.js";

// Rich per-cluster cards: accuracy gauge, share of total students, focus-area chips.
// Pure: receives `groups`, `t`, `locale`.
export const ClusterDetailCards = ({ groups, t, locale }) => {
    const total = (groups || []).reduce((sum, g) => sum + (g.memberCount || 0), 0);

    return (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 16 }}>
            {(groups || []).map((g, i) => {
                const color = clusterColor(i);
                const pct = accuracyPct(g.avgAccuracy);
                const share = total > 0 ? Math.round((g.memberCount / total) * 100) : 0;
                const focus = g.topErrors || [];
                return (
                    <div key={g.clusterId ?? i} className="adm-card" style={{ padding: 0, overflow: "hidden" }}>
                        <div style={{ height: 5, background: color }} />
                        <div style={{ padding: 18 }}>
                            {/* Header */}
                            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 14 }}>
                                <span style={{ width: 9, height: 9, borderRadius: "50%", background: color, flexShrink: 0 }} />
                                <h3 style={{ margin: 0, fontSize: 15, color: "var(--adm-txt)", flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", fontWeight: 700 }}>
                                    {clusterName(g.label, `#${g.clusterId}`)}
                                </h3>
                                <span style={{
                                    fontSize: 10.5, fontWeight: 700, color: "var(--adm-txt-muted)",
                                    background: "var(--adm-th-bg)", borderRadius: 7,
                                    padding: "2px 7px", flexShrink: 0, border: "1px solid var(--adm-bd)",
                                }}>#{g.clusterId}</span>
                            </div>

                            {/* Gauge + count */}
                            <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
                                <GaugeChart
                                    percent={pct ?? 0}
                                    color={pct == null ? COLORS.track : rateColor(pct)}
                                    size={104}
                                    label={t.statAvgAccuracy}
                                />
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div style={{ fontSize: 28, fontWeight: 800, color: "var(--adm-txt)", lineHeight: 1 }}>
                                        {g.memberCount}
                                    </div>
                                    <div style={{ fontSize: 12, color: "var(--adm-txt-muted)" }}>{t.membersUnit}</div>
                                    {/* Share bar */}
                                    <div className="adm-pbar" style={{ marginTop: 10, marginBottom: 4 }}>
                                        <motion.div
                                            className="adm-pbar-fill"
                                            initial={{ width: 0 }}
                                            animate={{ width: `${share}%` }}
                                            transition={{ duration: 0.8, ease: "easeOut" }}
                                            style={{ background: color }}
                                        />
                                    </div>
                                    <div style={{ fontSize: 11, color: "var(--adm-txt-muted)" }}>
                                        {format(t.ofTotalShare, { percent: share })}
                                    </div>
                                </div>
                            </div>

                            {/* Focus areas */}
                            <div style={{ marginTop: 14 }}>
                                <div style={{ fontSize: 11, color: "var(--adm-txt-muted)", marginBottom: 6, fontWeight: 600, textTransform: "uppercase", letterSpacing: ".05em" }}>
                                    {t.focusAreas}
                                </div>
                                {focus.length > 0 ? (
                                    <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                                        {focus.map((code, ci) => (
                                            <motion.span
                                                key={code}
                                                initial={{ opacity: 0, y: 5 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                transition={{ delay: 0.12 + ci * 0.06 }}
                                                style={{
                                                    background: "var(--adm-surface-2)",
                                                    color: "var(--adm-txt-2)",
                                                    border: "1px solid var(--adm-bd)",
                                                    padding: "3px 10px", borderRadius: 13, fontSize: 12,
                                                }}
                                            >
                                                {humanizeError(code, t)}
                                            </motion.span>
                                        ))}
                                    </div>
                                ) : (
                                    <span style={{ fontSize: 12.5, color: "#10B981" }}>{t.noFocusAreas}</span>
                                )}
                            </div>

                            {g.lastAssignedAt && (
                                <div style={{ fontSize: 11, color: "var(--adm-txt-muted)", marginTop: 12 }}>
                                    {t.statLastRun}: {new Date(g.lastAssignedAt).toLocaleString(locale)}
                                </div>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
};
