import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { getMLGroups, runClustering } from "../../service/adminApi.js";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";
import { containerVariants, Card, COLORS } from "../Dashboard/components/DashboardPrimitives.jsx";
import { MLSummaryCards } from "../../components/Admin/MLGroups/MLSummaryCards.jsx";
import { ClusterDistributionChart } from "../../components/Admin/MLGroups/ClusterDistributionChart.jsx";
import { ClusterAccuracyChart } from "../../components/Admin/MLGroups/ClusterAccuracyChart.jsx";
import { ErrorPatternsChart } from "../../components/Admin/MLGroups/ErrorPatternsChart.jsx";
import { ClusterDetailCards } from "../../components/Admin/MLGroups/ClusterDetailCards.jsx";
import { MLGroupsTable } from "../../components/Admin/MLGroupsTable.jsx";

/**
 * Admin "ML Groups" — a visual analytics dashboard for the K-Means clustering run.
 * Owns data-fetching + the (admin-only) "run clustering" action, then composes
 * modular, animated sections (recharts visuals + framer-motion), mirroring the
 * Student Dashboard's design system. Each section is a pure presentational component.
 */
export const AdminMLGroups = () => {
    const { language, dir, locale } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null); // { lastRunAt, totalAssigned, groups }
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
        setRunning(true);
        setNotice("");
        setError("");
        runClustering()
            .then(() => { setNotice(t.clusteringRunSuccess); return load(); })
            .catch(() => setError(t.clusteringRunError))
            .finally(() => setRunning(false));
    };

    const groups = data?.groups || [];
    const hasGroups = groups.length > 0;
    const lastRun = data?.lastRunAt ? new Date(data.lastRunAt).toLocaleString(locale) : t.neverRun;

    return (
        <div dir={dir} style={page}>
            {/* ── Hero header ───────────────────────────────────────────── */}
            <header style={hero}>
                <div style={{ minWidth: 0 }}>
                    <Link to="/admin/dashboard" style={backLink}>← {t.dashboardTitle}</Link>
                    <h1 style={h1}>{t.mlGroupsTitle}</h1>
                    <p style={subtitle}>{t.mlSubtitle}</p>
                    <div style={metaRow}>
                        <span>👥 {format(t.mlTotalAssigned, { count: data?.totalAssigned ?? 0 })}</span>
                        <span style={{ opacity: 0.5 }}>•</span>
                        <span>🕒 {t.statLastRun}: {lastRun}</span>
                    </div>
                </div>
                <RunButton onClick={handleRun} running={running} t={t} />
            </header>

            {(notice || error) && (
                <div style={{ marginBottom: 16 }}>
                    {notice && <Pill color={COLORS.success} bg="#eafbf0">{notice}</Pill>}
                    {error && <Pill color={COLORS.error} bg="#fdecec">{error}</Pill>}
                </div>
            )}

            {/* ── Body: loading / empty / dashboard ─────────────────────── */}
            {loading ? (
                <SkeletonDashboard />
            ) : !hasGroups ? (
                // Empty state only after a successful load (data present); on a load
                // error `data` is null and the error pill above already explains.
                data ? <EmptyState t={t} onRun={handleRun} running={running} /> : null
            ) : (
                <motion.div variants={containerVariants} initial="hidden" animate="show" style={sections}>
                    <MLSummaryCards data={data} t={t} locale={locale} />

                    <div style={twoCol}>
                        <ClusterDistributionChart groups={groups} t={t} />
                        <ClusterAccuracyChart groups={groups} t={t} />
                    </div>

                    <ErrorPatternsChart groups={groups} t={t} />

                    <div>
                        <h2 style={sectionTitle}>{t.breakdownTitle}</h2>
                        <ClusterDetailCards groups={groups} t={t} locale={locale} />
                    </div>

                    <Card title={t.detailsTitle}>
                        <div style={{ overflowX: "auto" }}>
                            <MLGroupsTable groups={groups} t={t} locale={locale} />
                        </div>
                    </Card>
                </motion.div>
            )}
        </div>
    );
};

// ── sub-components ────────────────────────────────────────────────────────────

const RunButton = ({ onClick, running, t }) => (
    <motion.button
        onClick={onClick}
        disabled={running}
        whileHover={running ? undefined : { scale: 1.04 }}
        whileTap={running ? undefined : { scale: 0.97 }}
        style={{ ...runBtn, ...(running ? runBtnDisabled : null) }}
    >
        {running ? `⏳ ${t.runningClustering}` : `▶ ${t.runClustering}`}
    </motion.button>
);

const Pill = ({ color, bg, children }) => (
    <div style={{ color, background: bg, border: `1px solid ${color}33`, borderRadius: 10, padding: "10px 14px", fontSize: 14, marginBottom: 8 }}>
        {children}
    </div>
);

const EmptyState = ({ t, onRun, running }) => (
    <Card style={{ textAlign: "center", padding: "48px 24px" }}>
        <div style={{ fontSize: 48, marginBottom: 12 }}>🧩</div>
        <h2 style={{ margin: "0 0 8px", fontSize: 20, color: "#2b2b35" }}>{t.emptyTitle}</h2>
        <p style={{ margin: "0 auto 20px", maxWidth: 420, color: COLORS.muted, fontSize: 14, lineHeight: 1.5 }}>{t.emptyBody}</p>
        <RunButton onClick={onRun} running={running} t={t} />
    </Card>
);

const SkeletonDashboard = () => (
    <div style={sections}>
        <Skeleton h={170} />
        <div style={twoCol}><Skeleton h={320} /><Skeleton h={320} /></div>
        <Skeleton h={240} />
    </div>
);

const Skeleton = ({ h }) => (
    <motion.div
        animate={{ opacity: [0.5, 1, 0.5] }}
        transition={{ duration: 1.3, repeat: Infinity, ease: "easeInOut" }}
        style={{ height: h, borderRadius: 16, background: "#eef1f5" }}
    />
);

// ── styles ────────────────────────────────────────────────────────────────────
const page = { padding: 24, maxWidth: 1100, margin: "0 auto", fontFamily: "Arial, sans-serif" };
const sections = { display: "flex", flexDirection: "column", gap: 18 };
const twoCol = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 18 };
const hero = {
    display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 16, flexWrap: "wrap",
    padding: "22px 24px", borderRadius: 18, marginBottom: 18,
    background: "linear-gradient(135deg,#f3f8ff 0%, #eef2ff 100%)", border: "1px solid #e6ecf8",
};
const backLink = { fontSize: 13, color: COLORS.primary, textDecoration: "none", display: "inline-block", marginBottom: 6 };
const h1 = { margin: "0 0 4px", fontSize: 26, color: "#1e2330" };
const subtitle = { margin: 0, fontSize: 14, color: "#6a7180" };
const metaRow = { display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap", fontSize: 13, color: "#667085", marginTop: 12 };
const sectionTitle = { margin: "4px 2px 12px", fontSize: 18, color: "#2b2b35" };
const runBtn = {
    border: "none", borderRadius: 12, padding: "12px 20px", fontSize: 14, fontWeight: 700, color: "#fff",
    cursor: "pointer", background: "linear-gradient(135deg,#007bff,#5b8def)",
    boxShadow: "0 6px 16px rgba(0,123,255,0.30)", whiteSpace: "nowrap",
};
const runBtnDisabled = { opacity: 0.7, cursor: "default", boxShadow: "none" };
