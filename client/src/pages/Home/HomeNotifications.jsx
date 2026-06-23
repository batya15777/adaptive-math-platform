import { useEffect, useState } from 'react';
import { getNotifications } from '../../service/authApi.js';

// Admin broadcast messages from the last 7 days, surfaced on the Home dashboard.
// Self-contained: it fetches its own data (GET /notifications) so the Home page only
// has to drop it in. The live SSE toast (BroadcastAlert in DashboardLayout) is a
// separate, real-time channel — this card is the persistent 7-day history.
export const HomeNotifications = ({ t, locale }) => {
    const [notifications, setNotifications] = useState(null); // null = loading
    const [notifError, setNotifError] = useState(false);

    useEffect(() => {
        let active = true;
        getNotifications()
            .then(r => { if (active) setNotifications(r.data || []); })
            .catch(() => { if (active) { setNotifications([]); setNotifError(true); } });
        return () => { active = false; };
    }, []);

    const fmtDate = (iso) => {
        try {
            return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
        } catch { return iso; }
    };

    return (
        <section className="sc-card home-card home-notifs">
            <div className="home-card-h"><span className="ic">📨</span>{t.notificationsHistory}</div>

            {notifications === null && <p className="home-action-sub">{t.notificationsLoading}</p>}
            {notifError && <p className="home-action-sub home-msg--err">{t.loadError}</p>}
            {notifications !== null && !notifError && notifications.length === 0 && (
                <p className="home-action-sub">{t.notificationsEmpty}</p>
            )}

            {notifications && notifications.length > 0 && (
                <ul className="home-notif-list">
                    {notifications.map((n) => (
                        <li key={n.id} className="home-notif">
                            <span className="home-notif-ico" aria-hidden="true">📢</span>
                            <div className="home-notif-body">
                                <div className="home-notif-msg">{n.message}</div>
                                <div className="home-notif-date">{fmtDate(n.sentAt)}</div>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
};
