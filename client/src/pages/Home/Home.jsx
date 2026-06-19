import { useContext } from 'react';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { Leaderboard } from '../../components/Leaderboard/Leaderboard.jsx';

export const Home = () => {
    const { user } = useContext(AuthContext);
    const displayName = user?.username || user?.fullName || 'Student';

    return (
        <div style={page}>
            <div style={welcomeBox}>
                <h1 style={welcomeTitle}>Welcome back, {displayName}! 👋</h1>
                <p style={welcomeSub}>Keep practicing to climb the leaderboard.</p>
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
