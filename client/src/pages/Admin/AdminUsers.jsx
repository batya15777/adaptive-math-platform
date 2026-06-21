import { useState, useEffect } from "react";
import { getUsers } from "../../service/adminApi.js";
import { UsersTable } from "../../components/Admin/UsersTable.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";

const PAGE_SIZE = 20;

// "Smart" page: owns data fetching, server-side search/filter/pagination and
// loading/error state. The table itself is a separate presentational component.
export const AdminUsers = () => {
    const { language, locale } = useLanguage();
    const t = getAdminStrings(language);

    const [data, setData] = useState({ users: [], page: 0, totalPages: 0, totalElements: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");
    const [debouncedSearch, setDebouncedSearch] = useState("");
    const [role, setRole] = useState(""); // "" = All

    // Debounce the search box (setState lives in the timer callback, not the effect body).
    useEffect(() => {
        const id = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(id);
    }, [search]);

    useEffect(() => {
        let active = true;
        getUsers(page, PAGE_SIZE, debouncedSearch, role)
            .then((res) => { if (active) { setData(res.data); setError(""); } })
            .catch(() => { if (active) setError(t.usersLoadError); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [page, debouncedSearch, role, t.usersLoadError]);

    // Page reset + loading flag live in the handlers (not in the effect body) for eslint.
    const goToPage = (next) => { setLoading(true); setPage(next); };
    const onSearch = (v) => { setSearch(v); setPage(0); setLoading(true); };
    const onRole = (v) => { setRole(v); setPage(0); setLoading(true); };

    const hasResults = data.users.length > 0;

    return (
        <div style={{ padding: 24, fontFamily: "Arial, sans-serif" }}>
            <h1>{t.usersTitle}</h1>

            <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 16, flexWrap: "wrap" }}>
                <input
                    type="text"
                    value={search}
                    onChange={(e) => onSearch(e.target.value)}
                    placeholder={t.searchPlaceholder}
                    style={{ padding: 8, minWidth: 260 }}
                />
                <label style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                    <span style={{ fontSize: 14, color: "#6c757d" }}>{t.filterRole}:</span>
                    <select value={role} onChange={(e) => onRole(e.target.value)} style={{ padding: 6 }}>
                        <option value="">{t.filterAll}</option>
                        <option value="STUDENT">{t.filterStudents}</option>
                        <option value="ADMIN">{t.filterAdmins}</option>
                    </select>
                </label>
            </div>

            {error && <p style={{ color: "#dc3545" }}>{error}</p>}
            {loading ? (
                <p style={{ color: "#888" }}>{t.loading}</p>
            ) : hasResults ? (
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
            ) : (
                <p style={{ color: "#888" }}>{t.noResults}</p>
            )}
        </div>
    );
};
