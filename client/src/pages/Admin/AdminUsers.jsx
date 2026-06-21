import { useState, useEffect } from "react";
import { getUsers } from "../../service/adminApi.js";
import { UsersTable } from "../../components/Admin/UsersTable.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

const PAGE_SIZE = 20;

// "Smart" page: owns data fetching, pagination and loading/error state.
// The table itself is a separate presentational component (UsersTable).
export const AdminUsers = () => {
    const { language, locale } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState({ users: [], page: 0, totalPages: 0, totalElements: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let active = true;
        getUsers(page, PAGE_SIZE)
            .then((res) => { if (active) { setData(res.data); setError(""); } })
            .catch(() => { if (active) setError(t.usersLoadError); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [page, t.usersLoadError]);

    // setLoading lives in the handler (not in the effect body) to keep eslint happy.
    const goToPage = (next) => { setLoading(true); setPage(next); };

    return (
        <div style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <h1>{t.usersTitle}</h1>
            {error && <p style={{ color: "#dc3545" }}>{error}</p>}
            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
            ) : (
                <>
                    <UsersTable users={data.users} t={t} locale={locale} />

                    <div style={{ marginTop: 16, display: "flex", gap: 12, alignItems: "center" }}>
                        <button onClick={() => goToPage(page - 1)} disabled={page <= 0}>{t.prev}</button>
                        <span>
                            {format(t.usersPageStatus, {
                                page: data.page + 1,
                                total: Math.max(data.totalPages, 1),
                                count: data.totalElements,
                            })}
                        </span>
                        <button onClick={() => goToPage(page + 1)} disabled={page >= data.totalPages - 1}>{t.next}</button>
                    </div>
                </>
            )}
        </div>
    );
};
