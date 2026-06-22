import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { register, verifyCode } from "../../service/authApi.js";
import { useLanguage } from "../../i18n/useLanguage.js";
import { format } from "../../i18n/languages.js";
import { getAuthStrings } from "./authStrings.js";
import { AuthLayout } from "./AuthLayout.jsx";
import { Field, PasswordField, Otp, AgeSelect } from "./authFields.jsx";

function RegisterForm() {
    const { language } = useLanguage();
    const t = getAuthStrings(language);

    const [fullName, setFullName] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [email, setEmail] = useState("");
    const [age, setAge] = useState("");
    const [gender, setGender] = useState("");
    const [code, setCode] = useState("");
    const [showCode, setShowCode] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [errors, setErrors] = useState("");
    const [resendCooldown, setResendCooldown] = useState(0);

    const navigate = useNavigate();

    const fullNameRegex = (value) => {
        const checkFullName = /^[A-Za-z]+(?: [A-Za-z]+)+$/;
        return checkFullName.test(value.trim());
    };
    const passwordRegex = (value) => {
        const checkPassword = /^[A-Za-z!0-9@#*]{2,10}$/;
        return checkPassword.test(value.trim());
    };
    const emailRegex = (value) => {
        const checkEmail = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        return checkEmail.test(value.trim());
    };

    const validation = () => {
        let hasErrors = false;
        if (!fullNameRegex(fullName)) {
            hasErrors = true;
        }
        if (!passwordRegex(password)) {
            hasErrors = true;
        }
        if (password !== confirmPassword) {
            hasErrors = true;
        }
        if (!emailRegex(email)) {
            hasErrors = true;
        }
        if (age === "" || Number(age) < 1 || Number(age) > 120) {
            hasErrors = true;
        }
        if (gender.trim().length === 0) {
            hasErrors = true;
        }
        return hasErrors;
    };

    const handleRegister = (e) => {
        e.preventDefault();
        setErrors("");
        if (validation()) return;

        const data = {
            fullName, password, email, age: Number(age), gender
        };

        register(data)
            .then(response => {
                if (response.data.success) {
                    setShowCode(true);
                }
            })
            .catch(error => {
                console.log(error);
                setErrors(t.registerFailed);
            });
    };

    const handleVerify = () => {
        const data = { code, email };
        verifyCode(data).then((response) => {
            if (response.data.success) {
                setShowCode(false);
                setErrors(t.registerOk);
                navigate("/login");
            }
        })
            .catch(error => {
                setErrors(t.invalidCode);
                console.log(error);
            });
    };

    // Re-sends the verification email by re-issuing the same registration request.
    const handleResend = () => {
        if (resendCooldown > 0) return;
        setErrors("");
        register({ fullName, password, email, age: Number(age), gender })
            .then(() => setResendCooldown(30))
            .catch(() => setErrors(t.registerFailed));
    };

    useEffect(() => {
        if (resendCooldown <= 0) return;
        const id = setInterval(() => setResendCooldown((c) => c - 1), 1000);
        return () => clearInterval(id);
    }, [resendCooldown]);

    // Age groups span kindergarten → university and beyond (backend validates 1–120).
    const ageGroups = [
        { label: t.ageGroupKids, from: 4, to: 11 },
        { label: t.ageGroupTeens, from: 12, to: 17 },
        { label: t.ageGroupAdults, from: 18, to: 25 },
        { label: t.ageGroupOlder, from: 26, to: 120 },
    ];

    // ---- Email verification step (centered solo card, no side panel) ----
    if (showCode) {
        const descBefore = format(t.verifyDesc, { email: "" }).trim();
        return (
            <AuthLayout>
                <div className="mg-form mg-form--center">
                    <div className="mg-mail">
                        <div className="mg-mailbox" aria-hidden="true">✉</div>
                        <span className="mg-badge" aria-hidden="true">✓</span>
                    </div>
                    <h1>{t.verifyTitle}</h1>
                    <p className="mg-sub">
                        {descBefore}<br /><b dir="ltr">{email}</b>
                    </p>

                    <Otp value={code} onChange={setCode} ariaLabel={t.verifyTitle} />

                    <div className="mg-resend">
                        {t.resendQuestion}{" "}
                        {resendCooldown > 0
                            ? <span>{format(t.resendIn, { seconds: resendCooldown })}</span>
                            : <button type="button" className="mg-link" onClick={handleResend}>{t.resend}</button>}
                    </div>

                    {errors && <div className="mg-alert mg-alert--err">{errors}</div>}

                    <button className="mg-cta" type="button" onClick={handleVerify} disabled={code.length < 6}>
                        {t.verify}
                    </button>

                    <button type="button" className="mg-link mg-back" onClick={() => navigate("/login")}>
                        ← {t.backToLogin}
                    </button>

                    <div className="mg-valid">{t.codeValidFor}</div>
                </div>
            </AuthLayout>
        );
    }

    return (
        <AuthLayout wide>
            <form className="mg-form mg-form--wide" onSubmit={handleRegister}>
                <div className="mg-chead">
                    <h1>{t.registerTitle}</h1>
                    <p className="mg-sub">{t.registerSubtitle}</p>
                </div>

                <Field label={t.fullNameLabel} icon="👤">
                    <input className="mg-field-input" type="text" value={fullName}
                        placeholder={t.fullNamePh} autoComplete="name"
                        onChange={(e) => setFullName(e.target.value)} />
                </Field>

                <Field label={t.emailLabel} icon="✉">
                    <input className="mg-field-input" type="email" value={email}
                        placeholder={t.emailPh} autoComplete="email"
                        onChange={(e) => setEmail(e.target.value)} />
                </Field>

                <PasswordField label={t.passwordLabel} value={password} placeholder={t.passwordPh}
                    show={showPassword} onToggle={() => setShowPassword((v) => !v)}
                    labels={{ show: t.show, hide: t.hide }} autoComplete="new-password"
                    onChange={(e) => setPassword(e.target.value)} />

                <PasswordField label={t.confirmLabel} value={confirmPassword} placeholder={t.confirmPh}
                    show={showConfirmPassword} onToggle={() => setShowConfirmPassword((v) => !v)}
                    labels={{ show: t.show, hide: t.hide }} autoComplete="new-password"
                    onChange={(e) => setConfirmPassword(e.target.value)} />

                <div className="mg-grid2">
                    <AgeSelect label={t.ageLabel} value={age} placeholder={t.agePlaceholder}
                        groups={ageGroups} onChange={setAge} />

                    <div className="mg-fld">
                        <span className="mg-lbl">{t.genderLabel}</span>
                        <div className="mg-seg" role="group" aria-label={t.genderLabel}>
                            <button type="button" aria-pressed={gender === "female"} onClick={() => setGender("female")}>♀ {t.female}</button>
                            <button type="button" aria-pressed={gender === "male"} onClick={() => setGender("male")}>{t.male} ♂</button>
                        </div>
                    </div>
                </div>

                {errors && <div className="mg-alert mg-alert--err">{errors}</div>}

                <button className="mg-cta" type="submit" disabled={validation()}>
                    {t.register}
                </button>

                <p className="mg-foot">
                    {t.haveAccount}
                    <Link className="mg-link" to="/login">{t.loginHere}</Link>
                </p>
            </form>
        </AuthLayout>
    );
}

export default RegisterForm;
