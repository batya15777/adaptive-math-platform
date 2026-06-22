import { useContext } from 'react';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useProfile } from '../../contexts/useProfile.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { Leaderboard } from '../../components/Leaderboard/Leaderboard.jsx';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import '../../styles/spaceTokens.css';
import './leaderboardPage.css';

// Dedicated "Top students" page. Themed (follows ProfileTheme) + RTL/LTR aware.
// The top bar (logo on the end, theme + language + logout at the start) is the shared
// AppTopBar — the logo links home, so no extra "back home" link is needed.
export const LeaderboardPage = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { dir } = useLanguage();
    const theme = (profileData.theme || 'LIGHT').toLowerCase();
    const myName = user?.fullName || user?.username || null;

    return (
        <div className="mg-space lbp" data-theme={theme} dir={dir}>
            <Stars />
            {/* Wide top bar so the logo pins to the screen's right edge and the controls
                to the left edge (same structure as Home / Daily). The table stays narrow. */}
            <div className="sc-content lbp-topbar"><AppTopBar /></div>
            <div className="sc-content lbp-inner">
                <Leaderboard highlightName={myName} />
            </div>
        </div>
    );
};
