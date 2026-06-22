import { useState, useEffect } from 'react';
import { getTop10 } from '../../service/leaderboardApi.js';
import { AVATARS } from '../../assets/avatars/index.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { getLeaderboardStrings } from './leaderboardStrings.js';
import './leaderboard.css';

const MEDALS = ['🥇', '🥈', '🥉'];

// Circular profile picture for a leaderboard row. `pictureId` indexes into AVATARS
// (UserProfile.pictureId on the server). Falls back to a default icon when the id is
// missing or out of range — e.g. a user who never set a profile picture.
const LeaderAvatar = ({ pictureId }) => {
    const src = pictureId == null ? undefined : AVATARS[pictureId];
    if (!src) {
        return <span className="lb-avatar lb-avatar--fallback" aria-hidden="true">👤</span>;
    }
    return <img className="lb-avatar" src={src} alt="" loading="lazy" />;
};

// Top-10 list. `highlightName` (optional) emphasises the current user's row.
export const Leaderboard = ({ highlightName }) => {
    const { language, locale } = useLanguage();
    const t = getLeaderboardStrings(language);

    const [entries, setEntries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error,   setError]   = useState('');

    useEffect(() => {
        getTop10()
            .then(res => setEntries(res.data?.leaderboard || []))
            .catch(() => setError(t.loadError))
            .finally(() => setLoading(false));
    }, [t.loadError]);

    return (
        <div className="sc-card lb">
            <div className="lb-head">
                <span className="lb-trophy" aria-hidden="true">🏆</span>
                <h2>{t.title}</h2>
            </div>

            {loading && <div className="lb-msg">{t.loading}</div>}
            {error && <div className="lb-msg lb-msg--err">{error}</div>}
            {!loading && !error && entries.length === 0 && <div className="lb-msg">{t.empty}</div>}

            {!loading && !error && entries.length > 0 && (
                <ul className="lb-list">
                    {entries.map((entry, i) => {
                        const isMe = highlightName && entry.fullName === highlightName;
                        return (
                            <li key={i} className={'lb-row' + (i < 3 ? ' lb-row--top' : '') + (isMe ? ' lb-row--me' : '')}>
                                <span className={'lb-rank' + (i < 3 ? ' lb-rank--medal' : '')}>
                                    {i < 3 ? MEDALS[i] : `#${i + 1}`}
                                </span>
                                <LeaderAvatar pictureId={entry.pictureId} />
                                <span className="lb-name">{entry.fullName}</span>
                                <span className="lb-stars">⭐ {(entry.totalStars ?? 0).toLocaleString(locale)}</span>
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
};
