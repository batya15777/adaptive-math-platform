import { useContext, useEffect } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useLanguage } from '../../i18n/useLanguage.js';

// Thin student shell: the survey gate + direction only. The themed header + primary nav
// live in the shared AppTopBar (rendered inside each page's own purple/space background),
// so there is no separate white bar or duplicate navigation here.
const DashboardLayout = () => {
    const { user, authLoading } = useContext(AuthContext);
    const navigate = useNavigate();
    const { dir } = useLanguage();

    // Survey gate: redirect to placement survey until it has been completed exactly once.
    // Guard against authLoading so a hard-refresh race condition (user briefly null) can't
    // produce a `survey_done_undefined` key that never matches and triggers a false redirect.
    useEffect(() => {
        if (authLoading) return;
        if (user && !localStorage.getItem(`survey_done_${user.id}`)) {
            navigate('/level-survey', { replace: true });
        }
    }, [user, authLoading, navigate]);

    return (
        <div dir={dir}>
            <Outlet />
        </div>
    );
};

export default DashboardLayout;
