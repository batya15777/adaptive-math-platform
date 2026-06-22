import { Outlet } from "react-router-dom";
import { useLanguage } from "../../i18n/useLanguage.js";

// Shared shell for every admin page: applies the active text direction (RTL/LTR)
// to the whole admin area, then renders the routed page via <Outlet/>.
// The language switcher itself lives once in the global Navbar.
export const AdminLayout = () => {
    const { dir } = useLanguage();

    return (
        <div dir={dir}>
            <Outlet />
        </div>
    );
};
