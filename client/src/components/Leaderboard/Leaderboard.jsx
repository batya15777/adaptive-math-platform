import { useState, useEffect } from 'react';
import { getTop10 } from '../../service/leaderboardApi.js';

const MEDALS   = ['🥇', '🥈', '🥉'];
const ROW_TINT = ['#fff8e1', '#f5f5f5', '#fff3e0']; // gold / silver / bronze tint for top 3

export const Leaderboard = () => {
    const [entries, setEntries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error,   setError]   = useState('');

    useEffect(() => {
        getTop10()
            .then(res => setEntries(res.data?.leaderboard || []))
            .catch(() => setError('Could not load the leaderboard.'))
            .finally(() => setLoading(false));
    }, []);

    return (
        <div style={container}>
            <div style={headerBar}>
                <span style={{ fontSize: '22px' }}>🏆</span>
                <h2 style={headerTitle}>Top 10 Students</h2>
            </div>

            {loading && (
                <div style={centeredMsg}>Loading leaderboard...</div>
            )}

            {error && (
                <div style={{ ...centeredMsg, color: '#dc3545' }}>{error}</div>
            )}

            {!loading && !error && entries.length === 0 && (
                <div style={centeredMsg}>No entries yet — be the first to earn stars!</div>
            )}

            {!loading && !error && entries.length > 0 && (
                <div>
                    {entries.map((entry, i) => (
                        <div key={i} style={row(i)}>
                            {/* Rank */}
                            <span style={rankCell(i)}>
                                {i < 3 ? MEDALS[i] : `#${i + 1}`}
                            </span>

                            {/* Name */}
                            <span style={nameCell(i)}>
                                {entry.fullName}
                            </span>

                            {/* Stars */}
                            <span style={starsCell}>
                                ⭐ {(entry.totalStars ?? 0).toLocaleString()}
                            </span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

// ── styles ───────────────────────────────────────────────────────────────────

const container = {
    border: '1px solid #e0e0e0',
    borderRadius: '10px',
    overflow: 'hidden',
    backgroundColor: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.07)',
};

const headerBar = {
    display: 'flex', alignItems: 'center', gap: '10px',
    padding: '14px 20px',
    backgroundColor: '#f8f9fa',
    borderBottom: '1px solid #eee',
};

const headerTitle = { margin: 0, fontSize: '17px', color: '#333' };

const centeredMsg = {
    padding: '24px', textAlign: 'center',
    color: '#888', fontSize: '14px',
};

const row = (i) => ({
    display: 'flex', alignItems: 'center', gap: '14px',
    padding: '11px 20px',
    backgroundColor: i < 3 ? ROW_TINT[i] : (i % 2 === 0 ? '#fff' : '#fafafa'),
    borderBottom: '1px solid #f0f0f0',
});

const rankCell = (i) => ({
    minWidth: '34px', textAlign: 'center',
    fontSize: i < 3 ? '22px' : '14px',
    fontWeight: 'bold', color: '#555',
    flexShrink: 0,
});

const nameCell = (i) => ({
    flex: 1,
    fontSize: '15px',
    fontWeight: i < 3 ? '600' : '400',
    color: '#222',
});

const starsCell = {
    fontSize: '14px',
    fontWeight: 'bold',
    color: '#e6a817',
    flexShrink: 0,
};
