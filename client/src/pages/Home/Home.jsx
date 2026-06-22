import { useContext } from 'react';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { Leaderboard } from '../../components/Leaderboard/Leaderboard.jsx';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getHomeStrings } from './homeStrings.js';

// Renders under DashboardLayout, which owns `dir` — so no dir on this root.
export const Home = () => {
    const { user } = useContext(AuthContext);
    const { language } = useLanguage();
    const t = getHomeStrings(language);
    const displayName = user?.username || user?.fullName || t.student;

    return (
        <div style={page}>
            <div style={welcomeBox}>
                <h1 style={welcomeTitle}>{format(t.welcomeBack, { name: displayName })}</h1>
                <p style={welcomeSub}>{t.keepPracticing}</p>
            </div>

            <Leaderboard />
        </div>
    );
};

// ── styles ────────────────────────────────────────────────────────────────────

const page = {
    padding: '24px',
    maxWidth: '640px',
    margin: '0 auto',
    fontFamily: 'Arial, sans-serif',
};

const welcomeBox = {
    marginBottom: '24px',
};

const welcomeTitle = {
    margin: '0 0 6px',
    fontSize: '24px',
    color: '#222',
};

const welcomeSub = {
    margin: 0,
    fontSize: '14px',
    color: '#888',
};
