import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { getMLGroups, runClustering } from "../../service/adminApi.js";
import { MLGroupsTable } from "../../components/Admin/MLGroupsTable.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

// "Smart" page: read-only view of existing clustering groups + an admin-only
// "run clustering" action. Does not contain any ML logic itself.
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

    const hasGroups = data && data.groups && data.groups.length > 0;

    return (
        <div dir={dir} style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <p style={{ marginTop: 0 }}><Link to="/admin/dashboard">{t.dashboardTitle} →</Link></p>
            <h1>{t.mlGroupsTitle}</h1>

            <button onClick={handleRun} disabled={running} style={{ marginBottom: 16 }}>
                {running ? t.runningClustering : t.runClustering}
            </button>

            {notice && <p style={{ color: "#28a745" }}>{notice}</p>}
            {error && <p style={{ color: "#dc3545" }}>{error}</p>}

            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
            ) : data ? (
                <>
                    <p style={{ color: "#6c757d", fontSize: 14 }}>
                        {format(t.mlTotalAssigned, { count: data.totalAssigned })}
                        {" · "}{t.colLastRun}: {data.lastRunAt ? new Date(data.lastRunAt).toLocaleString(locale) : "—"}
                    </p>
                    {hasGroups ? (
                        <MLGroupsTable groups={data.groups} t={t} locale={locale} />
                    ) : (
                        <p style={{ color: "#888" }}>{t.mlNoGroups}</p>
                    )}
                </>
            ) : null}
        </div>
    );
};
