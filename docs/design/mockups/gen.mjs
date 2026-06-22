import { writeFileSync } from "node:fs";

/* ---------- i18n strings (mirrors AUTH_STRINGS keys) ---------- */
const S = {
  he: {
    loginTitle: "ברוך שובך!", loginSubtitle: "התחבר כדי להמשיך",
    registerTitle: "צור חשבון חדש", registerSubtitle: "הצטרף למיליוני לומדים",
    emailLabel: "אימייל", passwordLabel: "סיסמה", fullNameLabel: "שם מלא",
    ageLabel: "גיל", genderLabel: "מין", rememberMe: "זכור אותי",
    agePlaceholder: "בחר גיל", haveAccount: "יש לך כבר חשבון?", loginHere: "התחבר",
    noAccount: "אין לך חשבון?", registerHere: "הרשמה",
    tagline: "למידה אדפטיבית שמובילה אותך",
    verifyTitle: "אימות מייל", verifyDesc: "שלחנו קוד אימות לכתובת",
    resendQuestion: "לא קיבלת קוד?", resend: "שלח שוב", verify: "אמת קוד",
    backToLogin: "חזור להתחברות", codeValidFor: "הקוד תקף ל-10 דקות",
    login: "התחבר", register: "הרשמה", female: "נקבה", male: "זכר",
    fullNamePh: "הכנס שם מלא", copyright: "© 2025 MathGalaxy · כל הזכויות שמורות",
    titles: ["1. התחברות", "2. הרשמה", "3. אימות מייל"],
  },
  en: {
    loginTitle: "Welcome back!", loginSubtitle: "Sign in to continue",
    registerTitle: "Create a new account", registerSubtitle: "Join millions of learners",
    emailLabel: "Email", passwordLabel: "Password", fullNameLabel: "Full name",
    ageLabel: "Age", genderLabel: "Gender", rememberMe: "Remember me",
    agePlaceholder: "Select age", haveAccount: "Already have an account?", loginHere: "Log in",
    noAccount: "Don't have an account?", registerHere: "Register",
    tagline: "Adaptive learning that guides you",
    verifyTitle: "Verify your email", verifyDesc: "We sent a verification code to",
    resendQuestion: "Didn't get a code?", resend: "Resend", verify: "Verify code",
    backToLogin: "Back to login", codeValidFor: "Code valid for 10 minutes",
    login: "Login", register: "Register", female: "Female", male: "Male",
    fullNamePh: "Enter full name", copyright: "© 2025 MathGalaxy · All rights reserved",
    titles: ["1. Login", "2. Register", "3. Email Verification"],
  },
};

/* deterministic starfield */
function stars(n, seed) {
  let s = seed, out = [];
  const rnd = () => (s = (s * 9301 + 49297) % 233280) / 233280;
  for (let i = 0; i < n; i++) {
    const x = (rnd() * 100).toFixed(2), y = (rnd() * 100).toFixed(2);
    const sz = rnd() < 0.12 ? 2 : 1, o = (0.35 + rnd() * 0.6).toFixed(2);
    out.push(`<i style="left:${x}%;top:${y}%;width:${sz}px;height:${sz}px;opacity:${o}"></i>`);
  }
  return out.join("");
}

const glyph = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none">
  <circle cx="12" cy="12" r="3.2" fill="#7C4DFF"/>
  <ellipse cx="12" cy="12" rx="10" ry="4.2" stroke="#A78BFA" stroke-width="1.3" opacity=".9"/>
  <ellipse cx="12" cy="12" rx="10" ry="4.2" stroke="#3B82F6" stroke-width="1.3" transform="rotate(60 12 12)" opacity=".7"/>
  <ellipse cx="12" cy="12" rx="10" ry="4.2" stroke="#A78BFA" stroke-width="1.3" transform="rotate(120 12 12)" opacity=".5"/></svg>`;

const lock = (t) => `<div class="brand"><div class="brandrow">${glyph}<span class="wm"><b>Math</b><span>Galaxy</span></span></div><div class="tag">${t.tagline}</div></div>`;
const langLabel = (lang) => (lang === "he" ? "Hebrew (עברית)" : "English");
const langMenu = () => `<div class="mg-menu">
    <div class="mg-item sel"><span>Hebrew (עברית)</span><span class="ck">✓</span></div>
    <div class="mg-item"><span>English</span></div>
    <div class="mg-item"><span>Russian (Русский)</span></div>
  </div>`;
// Header pieces are pinned to fixed physical corners (logo right, controls left)
// so they never swap sides or collide when the language changes.
const topbar = (lang, theme, openMenu = false) => `
  <a class="logo">${glyph}<span class="wm tb"><b>Math</b><span>Galaxy</span></span></a>
  <div class="controls">
    <button class="toggle" aria-label="theme">${theme === "dark" ? "🌙" : "☀️"}</button>
    <div class="langwrap">
      <button class="langbtn"><span class="gl">🌐</span><span>${langLabel(lang)}</span><span class="chev">▾</span></button>
      ${openMenu ? langMenu() : ""}
    </div>
  </div>`;
const field = (label, inner) => `<label class="fld"><span class="lbl">${label}</span><div class="inp">${inner}</div></label>`;
const cta = (txt) => `<button class="cta">${txt}</button>`;

function loginBody(t) {
  return `<div class="card">
    <div class="chead"><h1>${t.loginTitle}</h1><p class="sub">${t.loginSubtitle}</p></div>
    ${field(t.emailLabel, `<span class="ic">✉</span><span class="ph">example@email.com</span>`)}
    ${field(t.passwordLabel, `<span class="ic">🔒</span><span class="dots">••••••••</span><span class="eye">👁</span>`)}
    <label class="rem"><span class="cb"></span>${t.rememberMe}</label>
    ${cta(t.login)}
    <div class="foot">${t.noAccount} <a>${t.registerHere}</a></div>
  </div>`;
}
function registerBody(t) {
  return `<div class="card">
    <div class="chead"><h1>${t.registerTitle}</h1><p class="sub">${t.registerSubtitle}</p></div>
    ${field(t.fullNameLabel, `<span class="ic">👤</span><span class="ph">${t.fullNamePh}</span>`)}
    ${field(t.emailLabel, `<span class="ic">✉</span><span class="ph">example@email.com</span>`)}
    ${field(t.passwordLabel, `<span class="ic">🔒</span><span class="dots">••••••••</span><span class="eye">👁</span>`)}
    ${field(t.ageLabel, `<span class="ic">📅</span><span class="ph">${t.agePlaceholder}</span><span class="eye">▾</span>`)}
    <div class="fld"><span class="lbl">${t.genderLabel}</span>
      <div class="seg"><span class="on">♀ ${t.female}</span><span>${t.male} ♂</span></div></div>
    ${cta(t.register)}
    <div class="foot">${t.haveAccount} <a>${t.loginHere}</a></div>
  </div>`;
}
function verifyBody(t) {
  return `<div class="card center">
    <div class="mail"><div class="mailbox">✉</div><span class="badge">✓</span></div>
    <h1>${t.verifyTitle}</h1>
    <p class="sub">${t.verifyDesc}<br><b dir="ltr">example@email.com</b></p>
    <div class="otp">${[1,2,3,4,5,6].map((d,i)=>`<span class="${i===0?'on':''}">${d}</span>`).join("")}</div>
    <div class="resend">${t.resendQuestion} <a>${t.resend}</a></div>
    ${cta(t.verify)}
    <a class="back">← ${t.backToLogin}</a>
    <div class="valid">${t.codeValidFor}</div>
  </div>`;
}

function frame(bodyHtml, t, openMenu = false) {
  return `<div class="frame">
    <div class="bg"><div class="starfield">${stars(90, 7)}</div></div>
    <div class="formpane"><div class="scrim"></div>${bodyHtml}</div>
    <div class="artzone">
      <span class="planet"></span><span class="comet"></span>
      <div class="galaxy"></div>
      <div class="tag">${t.tagline}</div>
    </div>
    ${topbar(LANG, THEME, openMenu)}
    <div class="copy">${t.copyright}</div>
  </div>`;
}

let LANG = "he", THEME = "dark";

function page(lang, theme) {
  LANG = lang; THEME = theme;
  const t = S[lang], dir = lang === "he" ? "rtl" : "ltr";
  const frames = [loginBody(t), registerBody(t), verifyBody(t)].map((b, i) =>
    `<div class="cell"><div class="ftitle">${t.titles[i]}</div>${frame(b, t)}</div>`).join("");
  return `<!doctype html><html dir="${dir}" data-theme="${theme}" lang="${lang}"><head><meta charset="utf-8">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Rubik:wght@400;500;600;700&family=Heebo:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>${CSS}</style></head>
<body><div class="board">
  <div class="bhead"><span class="dot"></span> MathGalaxy · Auth · <b>${theme === "dark" ? "Dark" : "Light"}</b> · ${lang === "he" ? "Hebrew (RTL)" : "English (LTR)"}</div>
  <div class="row">${frames}</div>
</div></body></html>`;
}

const CSS = `
*{box-sizing:border-box;margin:0;padding:0}
:root{--accent:#7C4DFF;--accent-2:#3B82F6;--cta:linear-gradient(135deg,#7C4DFF,#3B82F6);
  --font:"Rubik","Heebo",Inter,system-ui,"Segoe UI",Roboto,sans-serif;--radius:16px}
:root[data-theme="dark"]{--bg:#0B1020;--card:#131A2E;--s2:#1A2340;--ts:#F4F6FB;--tx:#C7CDDB;--tm:#8B93A7;
  --bd:rgba(255,255,255,.10);--ring:rgba(124,77,255,.55);--glow:0 0 40px rgba(124,77,255,.30);
  --chip:rgba(20,26,46,.55);
  --cardsh:0 24px 70px rgba(0,0,0,.55);--scrim:rgba(11,16,32,.55);--boardbg:#070A14;--boardtx:#aeb6cc}
:root[data-theme="light"]{--bg:#EAF0FF;--card:#FFFFFF;--s2:#EEF1FA;--ts:#0B1020;--tx:#2C3450;--tm:#5B6480;
  --bd:rgba(11,16,32,.12);--ring:rgba(124,77,255,.45);--glow:0 0 30px rgba(124,77,255,.20);
  --chip:rgba(255,255,255,.72);
  --cardsh:0 20px 50px rgba(28,40,80,.18);--scrim:rgba(244,246,252,.30);--boardbg:#dfe6f5;--boardtx:#3a4566}
body{font-family:var(--font);background:var(--boardbg)}
.board{padding:30px 30px 34px}
.bhead{color:var(--boardtx);font-size:15px;font-weight:600;margin-bottom:18px;display:flex;align-items:center;gap:9px}
.dot{width:9px;height:9px;border-radius:50%;background:var(--cta);box-shadow:var(--glow)}
.row{display:flex;gap:26px}
.cell{width:620px}
.ftitle{color:var(--boardtx);font-size:13px;font-weight:600;margin:0 4px 9px;opacity:.85}
.frame{position:relative;display:flex;width:620px;height:760px;border-radius:22px;overflow:hidden;
  border:1px solid var(--bd);background:var(--bg);box-shadow:0 30px 80px rgba(0,0,0,.4)}

/* full-frame nebula background */
.bg{position:absolute;inset:0;z-index:0;
  background:
   radial-gradient(900px 520px at 22% 16%,rgba(124,77,255,.40),transparent 60%),
   radial-gradient(760px 520px at 78% 86%,rgba(59,130,246,.32),transparent 62%),
   radial-gradient(520px 520px at 38% 62%,rgba(170,90,255,.16),transparent 60%),
   linear-gradient(160deg,#0a0f22,#070a16)}
:root[data-theme="light"] .bg{background:
   radial-gradient(900px 520px at 22% 16%,rgba(124,77,255,.20),transparent 60%),
   radial-gradient(760px 520px at 78% 86%,rgba(59,130,246,.16),transparent 62%),
   radial-gradient(520px 520px at 38% 62%,rgba(170,90,255,.10),transparent 60%),
   linear-gradient(160deg,#cdd8f4,#e8edfb)}
.starfield i{position:absolute;background:#fff;border-radius:50%}
:root[data-theme="light"] .starfield i{background:#8390c0;opacity:.5!important}

/* art column (flows opposite the form column automatically in RTL/LTR) */
.artzone{position:relative;flex:1;z-index:1;overflow:hidden;display:flex;flex-direction:column;
  align-items:center;justify-content:center;gap:18px;padding:24px;text-align:center}
.galaxy{width:230px;height:150px;border-radius:50%;filter:blur(1px);
  background:radial-gradient(closest-side,rgba(255,247,237,.97),rgba(255,196,140,.6) 16%,rgba(170,90,255,.4) 40%,rgba(59,130,246,.2) 62%,transparent 74%);
  box-shadow:0 0 90px rgba(170,90,255,.5);transform:rotate(-16deg)}
.planet{position:absolute;width:240px;height:240px;inset-inline-start:-90px;bottom:-110px;border-radius:50%;
  background:radial-gradient(circle at 32% 30%,#3a2a6b,#140d2b 70%);box-shadow:inset -24px -16px 50px rgba(0,0,0,.6),0 0 60px rgba(124,77,255,.25)}
:root[data-theme="light"] .planet{background:radial-gradient(circle at 32% 30%,#b9a7e8,#8a78c8 70%);opacity:.55}
.comet{position:absolute;inset-inline-end:14%;top:12%;width:120px;height:2px;transform:rotate(28deg);
  background:linear-gradient(90deg,transparent,rgba(255,255,255,.9));border-radius:2px;opacity:.8}
.brandrow{display:flex;align-items:center;gap:10px;z-index:2}
.g{display:flex}
.wm{font-size:30px;font-weight:700;color:var(--ts);letter-spacing:-.5px;text-shadow:0 2px 18px rgba(0,0,0,.5)}
:root[data-theme="light"] .wm{text-shadow:0 2px 14px rgba(255,255,255,.7)}
.wm b{color:#fff}.wm span{color:var(--accent)}
:root[data-theme="light"] .wm b{color:#0B1020}
.tag{color:#aab2cc;font-size:14px;line-height:1.5;max-width:200px;z-index:2}
:root[data-theme="light"] .tag{color:#5B6480}

/* topbar — logo + controls pinned to fixed physical corners (never swap / collide) */
.logo{position:absolute;top:20px;right:24px;z-index:8;display:inline-flex;align-items:center;gap:9px;text-decoration:none}
.logo svg{width:26px;height:26px}
.wm.tb{font-size:21px;text-shadow:none}
.controls{position:absolute;top:20px;left:24px;z-index:8;display:flex;align-items:center;gap:16px}
.toggle{width:40px;height:40px;display:inline-flex;align-items:center;justify-content:center;background:var(--chip);
  border:1px solid var(--bd);border-radius:999px;font-size:16px;cursor:pointer;backdrop-filter:blur(8px)}
.langwrap{position:relative}
.langbtn{display:inline-flex;align-items:center;gap:8px;background:var(--chip);border:1px solid var(--bd);
  border-radius:999px;padding:9px 14px;color:var(--ts);font-size:14px;cursor:pointer;backdrop-filter:blur(8px)}
.langbtn .chev{opacity:.55;font-size:11px}
.mg-menu{position:absolute;top:calc(100% + 10px);left:0;min-width:212px;background:var(--card);
  border:1px solid var(--bd);border-radius:14px;box-shadow:var(--cardsh);padding:7px;z-index:40}
.mg-item{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 12px;
  border-radius:10px;color:var(--ts);font-size:14px}
.mg-item.sel{background:rgba(124,77,255,.16);font-weight:600}
.mg-item .ck{color:var(--accent)}

/* form column (card pushed below the header bar via padding) */
.formpane{position:relative;width:352px;display:flex;align-items:center;justify-content:center;
  padding-block:60px 38px;z-index:3}
.scrim{position:absolute;inset:0;background:radial-gradient(circle at center,var(--scrim),transparent 72%)}
.card{position:relative;width:304px;background:var(--card);border:1px solid var(--bd);
  border-radius:var(--radius);padding:24px 22px;box-shadow:var(--cardsh);z-index:2}
.card.center{text-align:center}
.chead{margin-bottom:16px}
.brandrow.sm{justify-content:center;margin-bottom:12px}
.wm.sm{font-size:20px}
.card h1{color:var(--ts);font-size:26px;font-weight:700;text-align:center;letter-spacing:-.5px}
.sub{color:var(--tm);font-size:14px;text-align:center;margin-top:6px;line-height:1.5}
.fld{display:block;margin-top:13px}
.lbl{display:block;color:var(--tm);font-size:12.5px;font-weight:600;margin-bottom:6px;text-align:start}
.inp{height:46px;background:var(--s2);border:1px solid var(--bd);border-radius:12px;display:flex;align-items:center;
  gap:9px;padding:0 13px;color:var(--tx);font-size:14px}
.ic{opacity:.8;font-size:15px}
.ph{color:var(--tm)}.dots{color:var(--tx);letter-spacing:2px;flex:1;text-align:start}
.eye{margin-inline-start:auto;opacity:.7}
.rem{display:flex;align-items:center;gap:9px;color:var(--tx);font-size:13.5px;margin:13px 2px 0;justify-content:flex-start}
.cb{width:18px;height:18px;border-radius:5px;border:1.5px solid var(--bd);background:var(--s2);display:inline-block}
.cta{width:100%;height:48px;margin-top:18px;border:none;border-radius:12px;background:var(--cta);color:#fff;
  font-family:var(--font);font-size:15px;font-weight:600;box-shadow:var(--glow);cursor:pointer}
.foot{text-align:center;color:var(--tm);font-size:13.5px;margin-top:16px}
.foot a,.resend a,.back{color:var(--accent);font-weight:600;cursor:pointer}
.seg{height:46px;background:var(--s2);border:1px solid var(--bd);border-radius:12px;display:flex;padding:4px;gap:4px}
.seg span{flex:1;display:flex;align-items:center;justify-content:center;font-size:13.5px;color:var(--tm);border-radius:9px}
.seg .on{background:rgba(124,77,255,.18);color:var(--ts);border:1px solid var(--ring)}

/* verify */
.mail{position:relative;width:78px;height:78px;margin:4px auto 14px}
.mailbox{width:78px;height:78px;border-radius:18px;background:linear-gradient(135deg,rgba(124,77,255,.25),rgba(59,130,246,.18));
  border:1px solid var(--ring);display:flex;align-items:center;justify-content:center;font-size:34px;box-shadow:var(--glow)}
.badge{position:absolute;inset-inline-end:-4px;bottom:-4px;width:26px;height:26px;border-radius:50%;background:#22C55E;
  color:#fff;display:flex;align-items:center;justify-content:center;font-size:14px;border:3px solid var(--card)}
.otp{display:flex;gap:9px;justify-content:center;margin:18px 0 6px}
.otp span{width:46px;height:54px;border-radius:11px;background:var(--s2);border:1px solid var(--bd);
  display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:600;color:var(--ts);font-variant-numeric:tabular-nums}
.otp .on{border-color:var(--accent);box-shadow:var(--glow)}
.resend{color:var(--tm);font-size:13.5px;margin-top:10px}
.back{display:inline-block;margin-top:16px;font-size:14px}
.valid{color:var(--tm);font-size:12.5px;margin-top:12px;opacity:.8}
.copy{position:absolute;bottom:0;inset-inline:0;text-align:center;padding:14px;color:var(--tm);font-size:11.5px;z-index:4}
`;

const combos = [["he","dark"],["en","light"],["en","dark"],["he","light"]];
for (const [lang, theme] of combos) {
  const f = `board-${theme}-${lang}.html`;
  writeFileSync(new URL(f, import.meta.url), page(lang, theme));
  console.log("wrote", f);
}

/* ---------- single-screen, large, for close review ---------- */
const SOLO_CSS = `
body{background:var(--boardbg);display:flex;align-items:center;justify-content:center;min-height:100vh;padding:40px 0}
.wrap{padding:0 40px}
/* Auto height so a tall card (Register) grows the frame instead of colliding with the header. */
.frame.solo{width:1040px;height:auto;min-height:660px;border-radius:26px}
.solo .formpane{width:470px;padding-block:100px 50px}   /* big top pad clears the pinned header */
.solo .card{width:372px;padding:28px 30px}
.solo .card h1{font-size:30px}
.solo .logo svg{width:28px;height:28px}
.solo .wm.tb{font-size:23px}
.solo .galaxy{width:300px;height:190px}
.solo .tag{font-size:16px;max-width:240px}
.solo .planet{width:300px;height:300px;inset-inline-start:-110px;bottom:-130px}
`;
function solo(screen, lang, theme) {
  LANG = lang; THEME = theme;
  const t = S[lang], dir = lang === "he" ? "rtl" : "ltr";
  const body = screen === "login" ? loginBody(t) : screen === "register" ? registerBody(t) : verifyBody(t);
  const inner = frame(body, t, screen === "login").replace('class="frame"', 'class="frame solo"');
  return `<!doctype html><html dir="${dir}" data-theme="${theme}" lang="${lang}"><head><meta charset="utf-8">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Rubik:wght@400;500;600;700&family=Heebo:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>${CSS}${SOLO_CSS}</style></head><body><div class="wrap">${inner}</div></body></html>`;
}
const screens = ["login", "register", "verify"];
for (const theme of ["dark", "light"]) {
  for (const screen of screens) {
    const f = `he-${screen}-${theme}.html`;
    writeFileSync(new URL(f, import.meta.url), solo(screen, "he", theme));
    console.log("wrote", f);
  }
}
