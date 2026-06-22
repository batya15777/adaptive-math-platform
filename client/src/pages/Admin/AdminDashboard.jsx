import { useState, useEffect, useContext } from "react";
import { Link } from "react-router-dom";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { getAnalytics, sendBroadcast } from "../../service/adminApi.js";
import { StatCard } from "../../components/Admin/StatCard.jsx";

// Real admin dashboard: read-only platform analytics (cards + simple tables).
// dir is provided by AdminLayout.
export const AdminDashboard = () => {
    const { user } = useContext(AuthContext);
    const { language } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [broadcastMsg, setBroadcastMsg] = useState("");

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
        sendBroadcast(msg).then(() => setBroadcastMsg(""));
    };

    return (
        <div style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <h1>{t.dashboardTitle}</h1>
            <p>{t.welcome}, {user?.username} ({t.roleLabel}: {user?.role}).</p>

            <div style={broadcastBox}>
                <textarea
                    value={broadcastMsg}
                    onChange={(e) => setBroadcastMsg(e.target.value)}
                    placeholder={t.broadcastPlaceholder}
                    rows={2}
                    style={broadcastTextarea}
                />
                <button onClick={handleBroadcast} disabled={!broadcastMsg.trim()} style={broadcastBtn}>
                    {t.broadcastSend}
                </button>
            </div>

            {error && <p style={{ color: "#dc3545" }}>{error}</p>}
            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
            ) : data ? (
                <>
                    <div style={cardsRow}>
                        <StatCard label={t.cardUsers} value={data.summary.totalUsers} />
                        <StatCard label={t.cardStudents} value={data.summary.totalStudents} />
                        <StatCard label={t.cardAttempts} value={data.summary.totalAttempts} />
                        <StatCard label={t.cardSuccessRate} value={`${data.summary.successRate}%`} />
                        <StatCard label={t.cardActive7d} value={data.summary.activeLearners7d} />
                    </div>

                    <h2 style={h2}>{t.hardestTitle}</h2>
                    {data.hardestSubSubjects.length ? (
                        <table style={table}>
                            <thead><tr>
                                <th style={th}>{t.colName}</th>
                                <th style={th}>{t.colAttempts}</th>
                                <th style={th}>{t.colSuccessRate}</th>
                            </tr></thead>
                            <tbody>
                                {data.hardestSubSubjects.map((s) => (
                                    <tr key={s.subSubjectId}>
                                        <td style={cell}>{s.name}</td>
                                        <td style={cell}>{s.attempts}</td>
                                        <td style={cell}>{s.successRate}%</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : <p style={muted}>{t.noData}</p>}

                    <h2 style={h2}>{t.commonErrorsTitle}</h2>
                    {data.commonErrors.length ? (
                        <table style={table}>
                            <thead><tr>
                                <th style={th}>{t.colError}</th>
                                <th style={th}>{t.colOccurrences}</th>
                            </tr></thead>
                            <tbody>
                                {data.commonErrors.map((e, i) => (
                                    <tr key={i}>
                                        <td style={cell}>{e.errorPattern}</td>
                                        <td style={cell}>{e.occurrences}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : <p style={muted}>{t.noData}</p>}

                    <h2 style={h2}>{t.leaderboardTitle}</h2>
                    {data.leaderboard.length ? (
                        <table style={table}>
                            <thead><tr>
                                <th style={th}>{t.colName}</th>
                                <th style={th}>{t.colStars}</th>
                            </tr></thead>
                            <tbody>
                                {data.leaderboard.map((u) => (
                                    <tr key={u.id}>
                                        <td style={cell}>{u.name}</td>
                                        <td style={cell}>⭐ {u.totalStars}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : <p style={muted}>{t.noData}</p>}
                </>
            ) : null}

            <p style={{ marginTop: 24 }}><Link to="/admin/users">{t.manageUsers}</Link></p>
            <p><Link to="/admin/topics">{t.manageTopics}</Link></p>
            <p><Link to="/admin/ml-groups">{t.manageClusters}</Link></p>
        </div>
    );
};

const cardsRow = { display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 8 };
const h2 = { marginTop: 24, fontSize: 18 };
const table = { width: "100%", borderCollapse: "collapse" };
const th = { textAlign: "start", borderBottom: "2px solid #ddd", padding: 8 };
const cell = { borderBottom: "1px solid #eee", padding: 8 };
const muted = { color: "#888" };
const broadcastBox = { marginBottom: 24, padding: 16, background: "#f8f9fa", borderRadius: 8, border: "1px solid #dee2e6" };
const broadcastTextarea = { width: "100%", padding: 8, fontSize: 14, borderRadius: 4, border: "1px solid #ced4da", resize: "vertical", boxSizing: "border-box", marginBottom: 8 };
const broadcastBtn = { padding: "8px 16px", background: "#1a73e8", color: "#fff", border: "none", borderRadius: 4, cursor: "pointer", fontSize: 14 };
