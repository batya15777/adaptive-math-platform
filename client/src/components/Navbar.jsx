import { useContext } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext.jsx";
import { logout } from "../service/authApi.js";

function Navbar() {

    const { user, logoutUser } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogout = () => {
        logout()
            .then(() => {
                logoutUser();
                navigate("/login"); // Redirect to login after logout
            })
            .catch(error => {
                console.log("שגיאה בהתנתקות:", error);
            });
    };

    if (!user) {
        return null;
    }


    return(
        <nav style={{ padding: "10px", backgroundColor: "#f0f0f0", marginBottom: "20px" }}>
            <span style={{ marginRight: "20px" }}>
                שלום, {user.username}!
            </span>
            <button onClick={handleLogout}>התנתק</button>
        </nav>
    );

}export default Navbar;
