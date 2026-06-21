import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { getMLGroups } from "../../service/adminApi.js";
import { MLGroupsTable } from "../../components/Admin/MLGroupsTable.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

// "Smart" page: read-only view of existing clustering groups. Does not run clustering.
export const AdminMLGroups = () => {
    const { language, dir, locale } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState(null); // { lastRunAt, totalAssigned, groups }
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let alive = true;
        getMLGroups()
            .then((res) => { if (alive) { setData(res.data); setError(""); } })
            .catch(() => { if (alive) setError(t.mlGroupsLoadError); })
            .finally(() => { if (alive) setLoading(false); });
        return () => { alive = false; };
    }, [t.mlGroupsLoadError]);

    const hasGroups = data && data.groups && data.groups.length > 0;

    return (
        <div dir={dir} style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <p style={{ marginTop: 0 }}><Link to="/admin/dashboard">{t.dashboardTitle} →</Link></p>
            <h1>{t.mlGroupsTitle}</h1>

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
