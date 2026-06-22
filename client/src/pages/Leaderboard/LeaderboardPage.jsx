import { useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useProfile } from '../../contexts/useProfile.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { getNavStrings } from '../../components/navStrings.js';
import { Leaderboard } from '../../components/Leaderboard/Leaderboard.jsx';
import { Stars } from '../../components/ui/Stars.jsx';
import { AppTopBar } from '../../components/ui/AppTopBar.jsx';
import '../../styles/spaceTokens.css';
import './leaderboardPage.css';

// Dedicated "Top students" page. Themed (follows ProfileTheme) + RTL/LTR aware.
export const LeaderboardPage = () => {
    const { user } = useContext(AuthContext);
    const { profileData } = useProfile();
    const { language, dir } = useLanguage();
    const nav = getNavStrings(language);
    const theme = (profileData.theme || 'LIGHT').toLowerCase();
    const myName = user?.fullName || user?.username || null;

    return (
        <div className="mg-space lbp" data-theme={theme} dir={dir}>
            <Stars />
            <div className="sc-content lbp-inner">
                <AppTopBar />
                <Link to="/home" className="lbp-back">← {nav.home}</Link>
                <Leaderboard highlightName={myName} />
            </div>
        </div>
    );
};
