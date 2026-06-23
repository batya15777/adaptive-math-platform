import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { useLanguage } from '../../i18n/useLanguage.js';
import { BroadcastAlert } from '../BroadcastAlert.jsx';

// Thin student shell: direction + broadcast alerts. Survey gate lives in SurveyGatedRoute
// in App.jsx (server-side flag), so no localStorage check needed here.
const DashboardLayout = () => {
    const { dir } = useLanguage();
    const [broadcasts, setBroadcasts] = useState([]);

    useEffect(() => {
        const es = new EventSource("http://localhost:8080/sse/user", { withCredentials: true });
        es.addEventListener("broadcast", (e) => setBroadcasts(prev => [...prev, e.data]));
        return () => es.close();
    }, []);

    return (
        <div dir={dir}>
            <BroadcastAlert
                messages={broadcasts}
                onDismiss={(i) => setBroadcasts(prev => prev.filter((_, idx) => idx !== i))}
            />
            <Outlet />
        </div>
    );
};

export default DashboardLayout;
