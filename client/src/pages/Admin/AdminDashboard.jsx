import { useState, useEffect, useContext } from "react";
import { Link } from "react-router-dom";
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell,
    RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis,
} from "recharts";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { useProfile } from "../../contexts/useProfile.js";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { getAnalytics, sendBroadcast } from "../../service/adminApi.js";
import { AVATARS } from "../../assets/avatars/index.js";

const CHART_COLORS = ["#7C4DFF", "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#06B6D4"];

// Avatar for leaderboard rows — mirrors the student Leaderboard component
const LeaderAvatar = ({ pictureId, name }) => {
    const src = pictureId == null ? undefined : AVATARS[pictureId];
    if (src) {
        return <img className="adm-lb-avatar" src={src} alt="" loading="lazy" />;
    }
    // Initials-based fallback with a deterministic gradient colour
    const colorIndex = (name?.charCodeAt(0) || 65) % CHART_COLORS.length;
    return (
        <span
            className="adm-lb-avatar--init"
            style={{ background: CHART_COLORS[colorIndex] }}
            aria-hidden="true"
        >
            {(name || "?").charAt(0).toUpperCase()}
        </span>
    );
};

const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
        <div className="adm-chart-tooltip">
            <div style={{ fontWeight: 600, marginBottom: 4 }}>{label}</div>
            {payload.map((p) => (
                <div key={p.dataKey} style={{ color: p.color || "#7C4DFF", fontSize: 12 }}>
                    {p.name}: <strong>{p.value}{typeof p.value === "number" && p.name?.includes("%") ? "%" : ""}</strong>
                </div>
            ))}
        </div>
    );
};

export const AdminDashboard = () => {
    const { user } = useContext(AuthContext);
    const { language, locale } = useLanguage();
    const { profileData } = useProfile();
    const t = getAdminStrings(language);
    const isDark = (profileData.theme || "LIGHT") === "DARK";

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [broadcastMsg, setBroadcastMsg] = useState("");
    const [broadcastDone, setBroadcastDone] = useState(false);

    useEffect(() => {
        let active = true;
        getAnalytics()
            .then((res) => { if (active) { setData(res.data); setError(""); } })
            .catch(() => { if (active) setError(t.analyticsLoadError); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [t.analyticsLoadError]);

    useEffect(() => {
        const es = new EventSource("http://localhost:8080/sse/admin", { withCredentials: true });
        es.addEventListener("analytics", (e) => setData(JSON.parse(e.data)));
        return () => es.close();
    }, []);

    const handleBroadcast = () => {
        const msg = broadcastMsg.trim();
        if (!msg) return;
        sendBroadcast(msg).then(() => { setBroadcastMsg(""); setBroadcastDone(true); setTimeout(() => setBroadcastDone(false), 3000); });
    };

    const axisStyle = { fill: isDark ? "#8B93A7" : "#6B7280", fontSize: 11 };
    const gridColor = isDark ? "rgba(255,255,255,.06)" : "rgba(11,16,32,.07)";
    const radarGrid = isDark ? "rgba(255,255,255,.12)" : "#e0e3f0";
    const radarAxis = isDark ? "#CBBBFF" : "#555";

    // Hardest sub-subjects — bar chart + table
    const hardestData = (data?.hardestSubSubjects || []).slice(0, 6).map((s) => ({
        name: s.name?.length > 14 ? s.name.slice(0, 13) + "…" : s.name,
        fullName: s.name,
        [t.colAttempts]: s.attempts,
        [t.colSuccessRate]: s.successRate,
    }));

    // Success / failure pie
    const successRate = data?.summary?.successRate ?? 0;
    const pieData = [
        { name: t.cardSuccessRate, value: successRate },
        { name: t.failLabel || "Not yet", value: Math.max(0, 100 - successRate) },
    ];
    const pieColors = ["#7C4DFF", isDark ? "#1A2340" : "#E8EAF4"];

    // Common errors horizontal bar
    const errorsData = (data?.commonErrors || []).slice(0, 8).map((e) => ({
        name: e.errorPattern?.length > 14 ? e.errorPattern.slice(0, 13) + "…" : e.errorPattern,
        fullName: e.errorPattern,
        [t.colOccurrences]: e.occurrences,
    }));

    // Leaderboard
    const leaderboard = data?.leaderboard || [];

    // Radar — subject success rates (up to 7 sub-subjects, showing performance spread)
    const radarData = (data?.hardestSubSubjects || []).slice(0, 7).map((s) => ({
        subject: s.name?.length > 18 ? s.name.slice(0, 17) + "…" : (s.name || ""),
        rate: s.successRate ?? 0,
    }));
    // Pad with platform-level KPIs if not enough topic axes
    if (radarData.length < 3 && data?.summary) {
        const { totalStudents = 0, totalUsers = 1, activeLearners7d = 0 } = data.summary;
        radarData.push(
            { subject: t.cardSuccessRate || "Success",   rate: successRate },
            { subject: t.cardActive7d    || "Active",    rate: Math.min(100, Math.round((activeLearners7d / Math.max(1, totalStudents)) * 100)) },
            { subject: t.cardStudents    || "Adoption",  rate: Math.min(100, Math.round((totalStudents / Math.max(1, totalUsers)) * 100)) },
        );
    }

    return (
        <div>
            {/* Page header */}
            <div className="adm-page-header">
                <div>
                    <h1 className="adm-page-title">📊 {t.dashboardTitle}</h1>
                    <p className="adm-page-subtitle">
                        {t.welcome}, <strong>{user?.username}</strong>
                    </p>
                </div>
                <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                    <Link to="/admin/users"     className="adm-btn adm-btn--ghost adm-btn--sm">👥 {t.manageUsers}</Link>
                    <Link to="/admin/topics"    className="adm-btn adm-btn--ghost adm-btn--sm">📚 {t.manageTopics}</Link>
                    <Link to="/admin/ml-groups" className="adm-btn adm-btn--ghost adm-btn--sm">🤖 {t.manageClusters}</Link>
                </div>
            </div>

            {error && <div className="adm-notice adm-notice--error">⚠ {error}</div>}

            {/* Stat cards */}
            {loading ? (
                <div className="adm-stats-row">
                    {[...Array(5)].map((_, i) => (
                        <div key={i} className="adm-skeleton" style={{ height: 110 }} />
                    ))}
                </div>
            ) : data && (
                <div className="adm-stats-row">
                    <StatCard icon="👤" value={data.summary.totalUsers.toLocaleString(locale)}        label={t.cardUsers}       color="#7C4DFF" />
                    <StatCard icon="🎓" value={data.summary.totalStudents.toLocaleString(locale)}     label={t.cardStudents}    color="#3B82F6" />
                    <StatCard icon="✏️"  value={data.summary.totalAttempts.toLocaleString(locale)}     label={t.cardAttempts}    color="#10B981" />
                    <StatCard icon="🎯" value={`${data.summary.successRate}%`}                        label={t.cardSuccessRate} color="#F59E0B" />
                    <StatCard icon="⚡" value={data.summary.activeLearners7d.toLocaleString(locale)}  label={t.cardActive7d}    color="#8B5CF6" />
                </div>
            )}

            {data && (
                <>
                    {/* ── Row 1: Hardest topics bar + Success rate pie ── */}
                    <div className="adm-charts-row" style={{ marginBottom: 20 }}>
                        <div className="adm-card">
                            <div className="adm-card-header">
                                <h3 className="adm-card-title">📉 {t.hardestTitle}</h3>
                                <span className="adm-card-subtitle">{t.colAttempts}</span>
                            </div>
                            <div className="adm-card-body">
                                {hardestData.length ? (
                                    <ResponsiveContainer width="100%" height={240}>
                                        <BarChart data={hardestData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
                                            <XAxis dataKey="name" tick={axisStyle} axisLine={false} tickLine={false} />
                                            <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
                                            <Tooltip content={<CustomTooltip />} />
                                            <Bar dataKey={t.colAttempts} fill="#7C4DFF" radius={[6, 6, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                ) : <div className="adm-empty">{t.noData}</div>}
                            </div>
                        </div>

                        <div className="adm-card">
                            <div className="adm-card-header">
                                <h3 className="adm-card-title">🎯 {t.cardSuccessRate}</h3>
                            </div>
                            <div className="adm-card-body" style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                                {/* Fixed 200×200 square ensures the donut is a geometrically perfect circle */}
                                <div style={{ position: "relative", display: "inline-block" }}>
                                    <PieChart width={200} height={200}>
                                        <Pie
                                            data={pieData}
                                            cx={100} cy={100}
                                            innerRadius={60} outerRadius={90}
                                            paddingAngle={3}
                                            dataKey="value"
                                            stroke="none"
                                        >
                                            {pieData.map((_, i) => <Cell key={i} fill={pieColors[i]} />)}
                                        </Pie>
                                        <Tooltip content={<CustomTooltip />} />
                                    </PieChart>
                                    {/* Overlay centered over the donut hole */}
                                    <div style={{
                                        position: "absolute",
                                        top: "50%", left: "50%",
                                        transform: "translate(-50%, -50%)",
                                        textAlign: "center",
                                        pointerEvents: "none",
                                        lineHeight: 1,
                                    }}>
                                        <div style={{ fontSize: 26, fontWeight: 800, color: "#7C4DFF" }}>{successRate}%</div>
                                        <div style={{ fontSize: 10, color: isDark ? "#8B93A7" : "#6B7280", marginTop: 3 }}>{t.cardSuccessRate}</div>
                                    </div>
                                </div>
                                {/* Legend */}
                                <div style={{ display: "flex", gap: 16, justifyContent: "center", marginTop: 4 }}>
                                    {pieData.map((entry, i) => (
                                        <span key={i} style={{ display: "flex", alignItems: "center", gap: 5, fontSize: 12, color: isDark ? "#8B93A7" : "#6B7280" }}>
                                            <span style={{ width: 8, height: 8, borderRadius: "50%", background: pieColors[i], display: "inline-block", flexShrink: 0 }} />
                                            {entry.name}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* ── Row 2: Common errors + Leaderboard with avatars ── */}
                    <div className="adm-charts-row" style={{ marginBottom: 20 }}>
                        <div className="adm-card adm-card--col">
                            <div className="adm-card-header">
                                <h3 className="adm-card-title">⚠ {t.commonErrorsTitle}</h3>
                            </div>
                            <div className="adm-card-body">
                                {errorsData.length ? (
                                    <>
                                        <ResponsiveContainer width="100%" height={Math.max(100, errorsData.length * 52 + 24)}>
                                            <BarChart data={errorsData} layout="vertical" margin={{ top: 4, right: 24, left: 10, bottom: 0 }}>
                                                <CartesianGrid strokeDasharray="3 3" stroke={gridColor} horizontal={false} />
                                                <XAxis type="number" tick={axisStyle} axisLine={false} tickLine={false} />
                                                <YAxis dataKey="name" type="category" tick={axisStyle} axisLine={false} tickLine={false} width={110} />
                                                <Tooltip content={<CustomTooltip />} />
                                                <Bar dataKey={t.colOccurrences} fill="#EF4444" radius={[0, 6, 6, 0]}>
                                                    {errorsData.map((_, i) => (
                                                        <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                                                    ))}
                                                </Bar>
                                            </BarChart>
                                        </ResponsiveContainer>
                                        <div className="adm-errors-insight">
                                            <span className="adm-errors-insight-total">
                                                {(t.commonErrorsTotal || "Total: {total} occurrences across {count} error types")
                                                    .replace("{total}", errorsData.reduce((s, e) => s + (e[t.colOccurrences] || 0), 0).toLocaleString(locale))
                                                    .replace("{count}", errorsData.length)}
                                            </span>
                                            <span className="adm-errors-insight-tip">
                                                💡 {t.commonErrorsInsight || "Focus on the top error patterns to improve student outcomes."}
                                            </span>
                                        </div>
                                    </>
                                ) : <div className="adm-empty">{t.noData}</div>}
                            </div>
                        </div>

                        {/* Leaderboard — styled to match the student Leaderboard component */}
                        <div className="adm-card">
                            <div className="adm-card-header" style={{ paddingBottom: 10, borderBottom: "1px solid var(--adm-bd)" }}>
                                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                                    <span style={{ fontSize: 20 }}>🏆</span>
                                    <h3 className="adm-card-title" style={{ margin: 0 }}>{t.leaderboardTitle}</h3>
                                </div>
                            </div>
                            {leaderboard.length ? (
                                <ul className="adm-lb-list" aria-label={t.leaderboardTitle}>
                                    {leaderboard.slice(0, 10).map((u, i) => (
                                        <li
                                            key={u.id ?? i}
                                            className={"adm-lb-row" + (i < 3 ? " adm-lb-row--top" : "")}
                                        >
                                            <span className={"adm-lb-rank" + (i < 3 ? " adm-lb-rank--medal" : "")}>
                                                {i < 3 ? ["🥇", "🥈", "🥉"][i] : `#${i + 1}`}
                                            </span>
                                            <LeaderAvatar pictureId={u.pictureId} name={u.name} />
                                            <span className="adm-lb-name">{u.name}</span>
                                            <span className="adm-lb-stars">⭐ {(u.totalStars ?? 0).toLocaleString(locale)}</span>
                                        </li>
                                    ))}
                                </ul>
                            ) : <div className="adm-empty">{t.noData}</div>}
                        </div>
                    </div>

                    {/* ── Row 3: Radar chart (subject performance) ── */}
                    {radarData.length >= 3 && (
                        <div className="adm-charts-row" style={{ marginBottom: 20 }}>
                            <div className="adm-card">
                                <div className="adm-card-header">
                                    <h3 className="adm-card-title">🕸 {t.radarTitle || "Subject Performance Radar"}</h3>
                                    <span className="adm-card-subtitle">{t.radarSubtitle || "Success rate across learning areas"}</span>
                                </div>
                                <div className="adm-card-body">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <RadarChart data={radarData} outerRadius="68%">
                                            <PolarGrid stroke={radarGrid} />
                                            <PolarAngleAxis
                                                dataKey="subject"
                                                tick={{ fontSize: 11, fill: radarAxis }}
                                            />
                                            <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
                                            <Radar
                                                dataKey="rate"
                                                name={t.cardSuccessRate || "Success Rate"}
                                                stroke="#7C4DFF"
                                                fill="#7C4DFF"
                                                fillOpacity={0.28}
                                                isAnimationActive
                                                animationDuration={900}
                                            />
                                            <Tooltip
                                                content={<CustomTooltip />}
                                                formatter={(v) => [`${v}%`, t.cardSuccessRate]}
                                            />
                                        </RadarChart>
                                    </ResponsiveContainer>
                                    <div className="adm-radar-legend">
                                        <i />
                                        <span>{t.cardSuccessRate || "Success Rate"} %</span>
                                    </div>
                                </div>
                            </div>

                            {/* Hardest topics table */}
                            {data.hardestSubSubjects.length > 0 && (
                                <div className="adm-card">
                                    <div className="adm-card-header">
                                        <h3 className="adm-card-title">📋 {t.hardestTitle}</h3>
                                    </div>
                                    <div className="adm-card-body adm-card-body--flush">
                                        <div className="adm-table-wrap">
                                            <table className="adm-table">
                                                <thead>
                                                    <tr>
                                                        <th>{t.colName}</th>
                                                        <th>{t.colAttempts}</th>
                                                        <th style={{ minWidth: 140 }}>{t.colSuccessRate}</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {data.hardestSubSubjects.map((s, i) => (
                                                        <tr key={s.subSubjectId}>
                                                            <td style={{ fontSize: 13 }}>{s.name}</td>
                                                            <td style={{ fontSize: 13 }}>{s.attempts.toLocaleString(locale)}</td>
                                                            <td>
                                                                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                                                                    <div className="adm-pbar" style={{ flex: 1 }}>
                                                                        <div className="adm-pbar-fill" style={{ width: `${s.successRate}%`, background: CHART_COLORS[i % CHART_COLORS.length] }} />
                                                                    </div>
                                                                    <span style={{ fontSize: 12, color: "var(--adm-txt-muted)", minWidth: 34, textAlign: "end" }}>{s.successRate}%</span>
                                                                </div>
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </>
            )}

            {/* Broadcast */}
            <div className="adm-broadcast">
                <div className="adm-broadcast-header">
                    <span style={{ fontSize: 18 }}>📢</span>
                    <span className="adm-broadcast-title">{t.broadcastSend}</span>
                </div>
                {broadcastDone && <div className="adm-notice adm-notice--success">✓ Message sent to all students.</div>}
                <textarea
                    value={broadcastMsg}
                    onChange={(e) => setBroadcastMsg(e.target.value)}
                    placeholder={t.broadcastPlaceholder}
                />
                <button
                    onClick={handleBroadcast}
                    disabled={!broadcastMsg.trim()}
                    className="adm-btn adm-btn--primary"
                >
                    📤 {t.broadcastSend}
                </button>
            </div>
        </div>
    );
};

const StatCard = ({ icon, value, label, color }) => (
    <div className="adm-stat-card">
        <div className="adm-stat-top-bar" style={{ background: color }} />
        <div className="adm-stat-icon">{icon}</div>
        <div className="adm-stat-value">{value}</div>
        <div className="adm-stat-label">{label}</div>
    </div>
);
