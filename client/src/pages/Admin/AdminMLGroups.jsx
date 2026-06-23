import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { getMLGroups, runClustering } from "../../service/adminApi.js";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";
import { containerVariants } from "../Dashboard/components/DashboardPrimitives.jsx";
import { MLSummaryCards } from "../../components/Admin/MLGroups/MLSummaryCards.jsx";
import { ClusterDistributionChart } from "../../components/Admin/MLGroups/ClusterDistributionChart.jsx";
import { ClusterAccuracyChart } from "../../components/Admin/MLGroups/ClusterAccuracyChart.jsx";
import { ErrorPatternsChart } from "../../components/Admin/MLGroups/ErrorPatternsChart.jsx";
import { ClusterDetailCards } from "../../components/Admin/MLGroups/ClusterDetailCards.jsx";
import { MLGroupsTable } from "../../components/Admin/MLGroupsTable.jsx";

export const AdminMLGroups = () => {
    const { language, dir, locale } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [running, setRunning] = useState(false);
    const [notice, setNotice] = useState("");

    const load = useCallback(() => {
        return getMLGroups()
            .then((res) => { setData(res.data); setError(""); })
            .catch(() => setError(t.mlGroupsLoadError))
            .finally(() => setLoading(false));
    }, [t.mlGroupsLoadError]);

    useEffect(() => { load(); }, [load]);

    const handleRun = () => {
        if (!window.confirm(t.runClusteringConfirm)) return;
        setRunning(true); setNotice(""); setError("");
        runClustering()
            .then(() => { setNotice(t.clusteringRunSuccess); return load(); })
            .catch(() => setError(t.clusteringRunError))
            .finally(() => setRunning(false));
    };

    const groups = data?.groups || [];
    const hasGroups = groups.length > 0;
    const lastRun = data?.lastRunAt ? new Date(data.lastRunAt).toLocaleString(locale) : t.neverRun;

    return (
        <div dir={dir}>
            {/* Page header */}
            <div className="adm-page-header">
                <div>
                    <Link to="/admin/dashboard" className="adm-back-link" style={{ display: "inline-flex", marginBottom: 8 }}>
                        ← {t.dashboardTitle}
                    </Link>
                    <h1 className="adm-page-title">🤖 {t.mlGroupsTitle}</h1>
                    <p className="adm-page-subtitle">{t.mlSubtitle}</p>
                    <div style={{ display: "flex", gap: 14, alignItems: "center", flexWrap: "wrap", marginTop: 8, fontSize: 12.5, color: "var(--adm-txt-muted)" }}>
                        <span>👥 {format(t.mlTotalAssigned, { count: data?.totalAssigned ?? 0 })}</span>
                        <span style={{ opacity: .4 }}>•</span>
                        <span>🕒 {t.statLastRun}: {lastRun}</span>
                    </div>
                </div>
                <motion.button
                    className={"adm-btn adm-btn--primary" + (running ? " adm-btn--disabled" : "")}
                    onClick={handleRun}
                    disabled={running}
                    whileHover={running ? undefined : { scale: 1.03 }}
                    whileTap={running ? undefined : { scale: 0.97 }}
                    style={{ flexShrink: 0 }}
                >
                    {running ? `⏳ ${t.runningClustering}` : `▶ ${t.runClustering}`}
                </motion.button>
            </div>

            {notice && <div className="adm-notice adm-notice--success">✓ {notice}</div>}
            {error  && <div className="adm-notice adm-notice--error">⚠ {error}</div>}

            {loading ? (
                <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                    <div className="adm-skeleton" style={{ height: 170 }} />
                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 16 }}>
                        <div className="adm-skeleton" style={{ height: 320 }} />
                        <div className="adm-skeleton" style={{ height: 320 }} />
                    </div>
                    <div className="adm-skeleton" style={{ height: 240 }} />
                </div>
            ) : !hasGroups ? (
                data ? (
                    <div className="adm-card">
                        <div className="adm-empty">
                            <div className="adm-empty-icon">🧩</div>
                            <h3 style={{ margin: "0 0 8px", color: "var(--adm-txt)", fontSize: 17 }}>{t.emptyTitle}</h3>
                            <p style={{ margin: "0 auto 20px", maxWidth: 420, lineHeight: 1.5 }}>{t.emptyBody}</p>
                            <button className="adm-btn adm-btn--primary" onClick={handleRun} disabled={running}>
                                ▶ {t.runClustering}
                            </button>
                        </div>
                    </div>
                ) : null
            ) : (
                <motion.div variants={containerVariants} initial="hidden" animate="show"
                    style={{ display: "flex", flexDirection: "column", gap: 20 }}>
                    <MLSummaryCards data={data} t={t} locale={locale} />

                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 20 }}>
                        <ClusterDistributionChart groups={groups} t={t} />
                        <ClusterAccuracyChart groups={groups} t={t} />
                    </div>

                    <ErrorPatternsChart groups={groups} t={t} />

                    <div>
                        <h2 style={{ margin: "0 0 14px", fontSize: 16, fontWeight: 700, color: "var(--adm-txt)" }}>
                            {t.breakdownTitle}
                        </h2>
                        <ClusterDetailCards groups={groups} t={t} locale={locale} />
                    </div>

                    <div className="adm-card">
                        <div className="adm-card-header" style={{ paddingBottom: 12 }}>
                            <h3 className="adm-card-title">📋 {t.detailsTitle}</h3>
                        </div>
                        <MLGroupsTable groups={groups} t={t} locale={locale} />
                    </div>
                </motion.div>
            )}
        </div>
    );
};
