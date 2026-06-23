import { useState, useContext } from "react";
import { useNavigate, Link } from "react-router-dom";
import { emailRegex, passwordRegex } from "../../utils/validators.js";
import { login } from "../../service/authApi.js";
import { AuthContext } from "../../context/AuthContextSetup.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getAuthStrings } from "./authStrings.js";
import { AuthLayout } from "./AuthLayout.jsx";
import { Field, PasswordField } from "./authFields.jsx";

function LoginForm() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [remember, setRemember] = useState(false); // sent to /auth/login → longer session + persistent cookie
    const [errors, setErrors] = useState("");

    const { loginUser } = useContext(AuthContext);
    const navigate = useNavigate();
    const { language } = useLanguage();
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
    };

    const handleLogin = (e) => {
        e.preventDefault();
        setErrors("");

        if (validation()) {
            return;//אם יש שגיאות ולידציה נעצור שליחה
        }
        const data = { email, password, remember };

        login(data)
            .then(response => {
                if (response.data.success) {
                    const loggedInUser = response.data.loginData.user;
                    loginUser(loggedInUser);
                    // AuthRoute handles the redirect for all cases (admin/survey/home)
                    // so we do not call navigate() here — that would fire two concurrent
                    // navigations and trigger React Router's throttle warning.
                } else {
                    // Distinct message for a non-active (blocked/deleted) account.
                    const msg = response.data.message || "";
                    setErrors(/not active|inactive/i.test(msg) ? t.accountInactive : t.invalidCreds);
                }
            })
            .catch(error => {
                console.log(error);
                setErrors(t.network);
            });
    };

    return (
        <AuthLayout>
            <form className="mg-form" onSubmit={handleLogin}>
                <div className="mg-chead">
                    <h1>{t.loginTitle}</h1>
                    <p className="mg-sub">{t.loginSubtitle}</p>
                </div>

                <Field label={t.emailLabel} icon="✉">
                    <input
                        className="mg-field-input"
                        type="email"
                        value={email}
                        placeholder={t.emailPh}
                        autoComplete="email"
                        onChange={(e) => setEmail(e.target.value)}
                    />
                </Field>

                <PasswordField
                    label={t.passwordLabel}
                    value={password}
                    placeholder={t.passwordPh}
                    show={showPassword}
                    onToggle={() => setShowPassword((v) => !v)}
                    labels={{ show: t.show, hide: t.hide }}
                    autoComplete="current-password"
                    onChange={(e) => setPassword(e.target.value)}
                />

                <div className="mg-remember-row">
                    <label className="mg-remember">
                        <input
                            type="checkbox"
                            checked={remember}
                            onChange={(e) => setRemember(e.target.checked)}
                        />
                        {t.rememberMe}
                    </label>
                    <button type="button" className="mg-link mg-forgot" onClick={() => navigate("/forgot-password")}>
                        {t.forgotPassword}
                    </button>
                </div>

                {errors && <div className="mg-alert mg-alert--err">{errors}</div>}

                <button className="mg-cta" type="submit" disabled={validation()}>
                    {t.login}
                </button>

                <p className="mg-foot">
                    {t.noAccount}
                    <Link className="mg-link" to="/register">{t.registerHere}</Link>
                </p>
            </form>
        </AuthLayout>
    );
}

export default LoginForm;
