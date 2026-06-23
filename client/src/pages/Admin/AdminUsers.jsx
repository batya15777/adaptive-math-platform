import { useState, useEffect, useContext } from "react";
import { getUsers, updateUserRole, setUserStatus, updateUser } from "../../service/adminApi.js";
import { UsersTable } from "../../components/Admin/UsersTable.jsx";
import { UserEditModal } from "../../components/Admin/UserEditModal.jsx";
import { getAdminStrings } from "../../components/Admin/adminStrings.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";
import { AuthContext } from "../../context/AuthContextSetup.js";

const PAGE_SIZE = 20;

export const AdminUsers = () => {
    const { language, locale, dir } = useLanguage();
    const t = getAdminStrings(language);
    const { user } = useContext(AuthContext);

    const [data, setData] = useState({ users: [], page: 0, totalPages: 0, totalElements: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [search, setSearch] = useState("");
    const [debouncedSearch, setDebouncedSearch] = useState("");
    const [role, setRole] = useState("");
    const [status, setStatus] = useState("");
    const [version, setVersion] = useState(0);
    const [editing, setEditing] = useState(null);
    const [editError, setEditError] = useState("");

    useEffect(() => {
        const id = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(id);
    }, [search]);

    useEffect(() => {
        let active = true;
        getUsers(page, PAGE_SIZE, debouncedSearch, role, status)
            .then((res) => { if (active) { setData(res.data); setError(""); } })
            .catch(() => { if (active) setError(t.usersLoadError); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [page, debouncedSearch, role, status, version, t.usersLoadError]);

    const goToPage = (next) => { setLoading(true); setPage(next); };
    const onSearch = (v) => { setSearch(v); setPage(0); setLoading(true); };
    const onRole = (v) => { setRole(v); setPage(0); setLoading(true); };
    const onStatus = (v) => { setStatus(v); setPage(0); setLoading(true); };

    const onChangeRole = (u, nextRole) => {
        if (!window.confirm(t.changeRoleConfirm)) return;
        setNotice(""); setError("");
        updateUserRole(u.id, nextRole)
            .then(() => { setNotice(t.roleChangeSuccess); setVersion((v) => v + 1); })
            .catch((e) => setError(e.response?.status === 409 ? t.roleChangeBlocked : t.roleChangeError));
    };

    const onSetStatus = (u, nextStatus) => {
        const confirmMsg = nextStatus === "DELETED" ? t.softDeleteConfirm
            : nextStatus === "BLOCKED" ? t.blockConfirm : t.unblockConfirm;
        if (!window.confirm(confirmMsg)) return;
        setNotice(""); setError("");
        setUserStatus(u.id, nextStatus)
            .then(() => { setNotice(t.statusChangeSuccess); setVersion((v) => v + 1); })
            .catch((e) => setError(e.response?.status === 409 ? t.statusChangeBlocked : t.statusChangeError));
    };

    const onEdit = (u) => { setEditError(""); setEditing(u); };

    const onSubmitEdit = (values) => {
        setEditError("");
        updateUser(editing.id, values)
            .then(() => { setEditing(null); setNotice(t.userUpdateSuccess); setVersion((v) => v + 1); })
            .catch((e) => {
                const s = e.response?.status;
                setEditError(s === 409 ? t.emailInUse : s === 400 ? t.invalidDetails : t.userUpdateError);
            });
    };

    const hasResults = data.users.length > 0;

    return (
        <div dir={dir}>
            <div className="adm-page-header">
                <div>
                    <h1 className="adm-page-title">👥 {t.usersTitle}</h1>
                    <p className="adm-page-subtitle">
                        {data.totalElements > 0 && `${data.totalElements} ${t.filterAll.toLowerCase()}`}
                    </p>
                </div>
            </div>

            {notice && <div className="adm-notice adm-notice--success">✓ {notice}</div>}
            {error   && <div className="adm-notice adm-notice--error">⚠ {error}</div>}

            {/* Search + filter toolbar */}
            <div className="adm-toolbar">
                <div className="adm-search">
                    <span className="adm-search-icon">🔍</span>
                    <input
                        type="text"
                        value={search}
                        onChange={(e) => onSearch(e.target.value)}
                        placeholder={t.searchPlaceholder}
                    />
                </div>
                <div className="adm-filter-group">
                    <span className="adm-filter-label">{t.filterRole}:</span>
                    <select className="adm-select" value={role} onChange={(e) => onRole(e.target.value)}>
                        <option value="">{t.filterAll}</option>
                        <option value="STUDENT">{t.filterStudents}</option>
                        <option value="ADMIN">{t.filterAdmins}</option>
                    </select>
                </div>
                <div className="adm-filter-group">
                    <span className="adm-filter-label">{t.filterStatus}:</span>
                    <select className="adm-select" value={status} onChange={(e) => onStatus(e.target.value)}>
                        <option value="">{t.statusOptDefault}</option>
                        <option value="ACTIVE">{t.statusOptActive}</option>
                        <option value="BLOCKED">{t.statusOptBlocked}</option>
                        <option value="DELETED">{t.statusOptDeleted}</option>
                        <option value="ALL">{t.statusOptAll}</option>
                    </select>
                </div>
            </div>

            <div className="adm-card">
                {loading ? (
                    <div className="adm-loading">⏳ {t.loading}</div>
                ) : hasResults ? (
                    <>
                        <UsersTable
                            users={data.users}
                            t={t}
                            locale={locale}
                            onChangeRole={onChangeRole}
                            onSetStatus={onSetStatus}
                            onEdit={onEdit}
                            currentUserId={user?.id}
                        />
                        <div className="adm-pagination">
                            <button
                                className="adm-btn adm-btn--ghost adm-btn--sm"
                                onClick={() => goToPage(page - 1)}
                                disabled={page <= 0}
                            >{t.prev}</button>
                            <span style={{ flex: 1, textAlign: "center" }}>
                                {format(t.usersPageStatus, {
                                    page: data.page + 1,
                                    total: Math.max(data.totalPages, 1),
                                    count: data.totalElements,
                                })}
                            </span>
                            <button
                                className="adm-btn adm-btn--ghost adm-btn--sm"
                                onClick={() => goToPage(page + 1)}
                                disabled={page >= data.totalPages - 1}
                            >{t.next}</button>
                        </div>
                    </>
                ) : (
                    <div className="adm-empty">
                        <div className="adm-empty-icon">🔍</div>
                        {t.noResults}
                    </div>
                )}
            </div>

            {editing && (
                <UserEditModal
                    key={editing.id}
                    initialUser={editing}
                    onSubmit={onSubmitEdit}
                    onClose={() => setEditing(null)}
                    dir={dir}
                    t={t}
                    error={editError}
                />
            )}
        </div>
    );
};
