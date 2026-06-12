import {useState , useContext} from "react";
import { useNavigate } from "react-router-dom";
import { emailRegex, passwordRegex } from "../utils/validators.js";
import { login } from "../service/authApi.js";
import { AuthContext } from "../context/AuthContextSetup.js";

function LoginForm() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [errors, setErrors] = useState("");

    const { loginUser } = useContext(AuthContext);
    const navigate = useNavigate();

    const validation = () => {
        let hasError = false;
        if (!emailRegex(email)) {
            hasError = true;
        }
        if (!passwordRegex(password)) {
            hasError = true;
        }
        return hasError;
    }

    const handleLogin = (e) => {
        e.preventDefault()
        setErrors("");

        if (validation()) {
            return;//אם יש שגיאות ולידציה נעצור שליחה
        }
        const data = {email, password};

        login(data)
            .then(response => {
                if (response.data.success) {
                    console.log(response.data);
                    console.log("Logged in!", response.data.loginData.user);

                    loginUser(response.data.loginData.user);
                    navigate("/"); // Redirect to dashboard
                } else {
                    setErrors("Email or Password are incorrect")
                }
            })
            .catch(error => {
                console.log(error)
                setErrors("שגיאת תקשורת עם השרת, אנא נסו שוב מאוחר יותר")
            });

    }

    return (

        <form onSubmit={handleLogin}>
            <input
                type="email"
                value={email}
                placeholder="Enter email"
                onChange={(e) => setEmail(e.target.value)}
            />

            <input
                type="password"
                value={password}
                placeholder="Enter password"
                onChange={(e) => setPassword(e.target.value)}
            />

            <button
                type="submit"
                disabled={validation()}
            >
                Login
            </button>

            {errors && <p style={{color: "red"}}>{errors}</p>}
        </form>
    );

}export default LoginForm;