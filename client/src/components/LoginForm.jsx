import {useState , useContext} from "react";
import { useNavigate, Link } from "react-router-dom";
import { emailRegex, passwordRegex } from "../utils/validators.js";
import { login } from "../service/authApi.js";
import { AuthContext } from "../context/AuthContextSetup.js";
import { useLanguage } from "../i18n/useLanguage.js";
import { getAuthStrings } from "./authStrings.js";
import { LanguageSwitcher } from "./LanguageSwitcher/LanguageSwitcher.jsx";
import "./LoginForm.css";

function LoginForm() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [errors, setErrors] = useState("");

    const { loginUser } = useContext(AuthContext);
    const navigate = useNavigate();
    const { language, dir } = useLanguage();
    const t = getAuthStrings(language);

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

                    const loggedInUser = response.data.loginData.user;
                    loginUser(loggedInUser);
                    navigate(loggedInUser?.role === "ADMIN" ? "/admin/dashboard" : "/");
                } else {
                    setErrors(t.invalidCreds)
                }
            })
            .catch(error => {
                console.log(error)
                setErrors(t.network)
            });

    }

    return (

        <form onSubmit={handleLogin} dir={dir}>
            <div style={{ textAlign: "end", marginBottom: 8 }}><LanguageSwitcher /></div>
            <input
                type="email"
                value={email}
                placeholder={t.emailPh}
                onChange={(e) => setEmail(e.target.value)}
            />

            <div>
                <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    placeholder={t.passwordPh}
                    onChange={(e) => setPassword(e.target.value)}
                />
                <button
                    type="button"
                    onClick={() => setShowPassword((currentValue) => !currentValue)}
                >
                    {showPassword ? t.hide : t.show}
                </button>
            </div>

            <button
                type="submit"
                disabled={validation()}
            >
                {t.login}
            </button>

            {errors && <p style={{color: "red"}}>{errors}</p>}

            <p className="login-register-hint">
                {t.noAccount}
                <Link to="/register">{t.registerHere}</Link>
            </p>
        </form>
    );

}export default LoginForm;