import { useContext, useEffect } from 'react';
import { Outlet, Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContextSetup.js';
// import { CalendarDays, BarChart3, Files, ShoppingCart, House, BookUser } from 'lucide-react';
// import styles from './DashboardLayout.module.css';

const DashboardLayout = () => {
    const { user, authLoading } = useContext(AuthContext);
    const navigate = useNavigate();

    // Survey gate: redirect to placement survey until it has been completed exactly once.
    // Guard against authLoading so a hard-refresh race condition (user briefly null) can't
    // produce a `survey_done_undefined` key that never matches and triggers a false redirect.
    useEffect(() => {
        if (authLoading) return;
        if (user && !localStorage.getItem(`survey_done_${user.id}`)) {
            navigate('/level-survey', { replace: true });
        }
    }, [user, authLoading, navigate]);

    // const location = useLocation();

    const navItems = [
        { name: 'Home', path: '/home'},
        { name: 'My Dashboard', path: '/dashboard'},
        { name: 'Math Training', path: '/math-training'},
        { name: 'settings', path: '/profile-settings'}
    ];

    return (
        <div >
            <aside >
                <div >
                    Mathematics Game
                </div>
                <nav >
                    {navItems.map((item) => {
                        // const Icon = item.icon;
                        // const isActive = location.pathname.includes(item.path);

                        return (
                            <Link
                                key={item.name}
                                to={item.path}

                            >
                                {/*<Icon size={20} />*/}
                                <span>{item.name}</span>
                            </Link>
                        );
                    })}
                </nav>
            </aside>

            <main >
                <Outlet />
            </main>
        </div>
    );
};

export default DashboardLayout;