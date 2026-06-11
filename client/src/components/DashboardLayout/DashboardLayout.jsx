import { Outlet, Link, useLocation } from 'react-router-dom';
// import { CalendarDays, BarChart3, Files, ShoppingCart, House, BookUser } from 'lucide-react';
// import styles from './DashboardLayout.module.css';

const DashboardLayout = () => {
    // const location = useLocation();

    const navItems = [
        { name: 'Home', path: '/home'},
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