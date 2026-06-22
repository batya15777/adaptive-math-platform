import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { forgotPassword, resetPassword } from "../../service/authApi.js";
import { emailRegex, passwordRegex } from "../../utils/validators.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { getAuthStrings } from "./authStrings.js";
import { AuthLayout } from "./AuthLayout.jsx";
import { Field, PasswordField, Otp } from "./authFields.jsx";

// Two-stage password reset:
//   "request" — enter email, receive a code.
//   "reset"   — enter the 6-digit code + a new password.
function ForgotPassword() {
    const { language } = useLanguage();
    const t = getAuthStrings(language);
    const navigate = useNavigate();

    const [stage, setStage] = useState("request");
    const [email, setEmail] = useState("");
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [notice, setNotice] = useState("");
    const [errors, setErrors] = useState("");
    const [loading, setLoading] = useState(false);

    const handleRequest = (e) => {
        e.preventDefault();
        setErrors("");
        if (!emailRegex(email)) return;
        setLoading(true);
        forgotPassword(email)
            .then((response) => {
                // Backend always returns success (no account enumeration).
                setNotice(response.data.message || t.resetSentMsg);
                setStage("reset");
            })
            .catch(() => setErrors(t.network))
            .finally(() => setLoading(false));
    };

    const handleReset = (e) => {
        e.preventDefault();
        setErrors("");
        if (code.length < 6 || !passwordRegex(password)) return;
        setLoading(true);
        resetPassword({ email, code, password })
            .then((response) => {
                if (response.data.success) {
                    navigate("/login");
                } else {
                    setErrors(response.data.message || t.invalidCode);
                }
            })
            .catch(() => setErrors(t.network))
            .finally(() => setLoading(false));
    };

    if (stage === "reset") {
        return (
            <AuthLayout>
                <form className="mg-form" onSubmit={handleReset}>
                    <div className="mg-chead">
                        <h1>{t.resetTitle}</h1>
                        <p className="mg-sub">{t.resetSubtitle}</p>
                    </div>

                    {notice && <div className="mg-alert mg-alert--info">{notice}</div>}

                    <Otp value={code} onChange={setCode} ariaLabel={t.resetTitle} />

                    <div style={{ marginTop: 8 }}>
                        <PasswordField
                            label={t.newPasswordLabel}
                            value={password}
                            placeholder={t.passwordPh}
                            show={showPassword}
                            onToggle={() => setShowPassword((v) => !v)}
                            labels={{ show: t.show, hide: t.hide }}
                            autoComplete="new-password"
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    {errors && <div className="mg-alert mg-alert--err">{errors}</div>}

                    <button className="mg-cta" type="submit" disabled={loading || code.length < 6 || !passwordRegex(password)}>
                        {t.resetSubmit}
                    </button>

                    <p className="mg-foot">
                        <Link className="mg-link" to="/login">← {t.backToLogin}</Link>
                    </p>
                </form>
            </AuthLayout>
        );
    }

    return (
        <AuthLayout>
            <form className="mg-form" onSubmit={handleRequest}>
                <div className="mg-chead">
                    <h1>{t.forgotTitle}</h1>
                    <p className="mg-sub">{t.forgotSubtitle}</p>
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

                {errors && <div className="mg-alert mg-alert--err">{errors}</div>}

                <button className="mg-cta" type="submit" disabled={loading || !emailRegex(email)}>
                    {t.sendResetCode}
                </button>

                <p className="mg-foot">
                    <Link className="mg-link" to="/login">← {t.backToLogin}</Link>
                </p>
            </form>
        </AuthLayout>
    );
}

export default ForgotPassword;
