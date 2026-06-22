# MathGalaxy — Auth Screens Design Spec
**Scope:** Login · Register · Email Verification
**Direction:** "Space Academy" — premium educational SaaS, dark-first **with full light-mode support**
**Status:** v3 (revised per requirements) — design spec for approval → implementation on branch `design-system`

> **What changed in v3** (per your requirements):
> 1. **Logo → top bar, single instance.** The MathGalaxy lockup is rendered **once**, in the top
>    header bar, on **all three screens**. It is **removed from the artwork area** and **removed from
>    the card header** — no duplication anywhere. See §IV.
> 2. **Theme — re-verified.** Screens follow the app's `ProfileTheme` (DARK/LIGHT), **not** the OS
>    setting. Verification statement added to §II.
> 3. **i18n — re-verified.** he/en/ru retained; RTL/LTR mirroring and long-string no-break confirmed
>    per screen. Verification statement added to §I.
> 4. **Font = Rubik** (confirmed). **Artwork stays CSS-based** (confirmed).
>
> **What changed in v2** (per your requirements):
> 1. **Full i18n** — every string comes from translation keys (extends the existing `AUTH_STRINGS`
>    dictionary); zero hardcoded text; verified in he/en/ru. See §I.
> 2. **Theme system** — screens honor the app's existing `ProfileTheme` (LIGHT/DARK), tokens only,
>    both modes fully defined. See §II.
> 3. **Age field** — redesigned to span 1st-grader → university student (and beyond). See §III.
> 4. **Google login removed** entirely for now (component, divider, and "or" chip all gone).
> 5. **Remember Me** — stays in the UI, **no backend behavior yet** (cosmetic placeholder). See §6 states.

---

## §I — Internationalization (hard requirement)

**Principle: not a single hardcoded user-facing string in the three screens.** Every label, title,
description, placeholder, button, link, helper, and error renders through the existing i18n pipeline.

**Reuse the existing infrastructure — do not invent a new one:**
- Source of truth: `client/src/components/authStrings.js` → `AUTH_STRINGS` (already has `en`/`he`/`ru`)
  consumed via `getAuthStrings(language)` (which wraps `getStrings(dict, code)` → English-fallback merge).
- Active language + direction come from `useLanguage()` → `{ language, dir, isRtl, locale }`, which
  reads `profileData.language` (one source of truth). Guest language persists via `readGuestLanguage`.
- Interpolation uses the existing `format("...{email}...", { email })` helper — e.g. the verification
  description and the resend countdown.

**New keys to add to `AUTH_STRINGS`** (en/he/ru each). Existing keys (`login`, `register`, `male`,
`female`, `invalidCreds`, `network`, `noAccount`, `registerHere`, …) are reused as-is:

| Key | en | he | ru |
|---|---|---|---|
| `loginTitle` | Welcome back! | ברוך שובך! | С возвращением! |
| `loginSubtitle` | Sign in to continue | התחבר כדי להמשיך | Войдите, чтобы продолжить |
| `registerTitle` | Create a new account | צור חשבון חדש | Создать аккаунт |
| `registerSubtitle` | Join millions of learners | הצטרף למיליוני לומדים | Присоединяйтесь к миллионам |
| `emailLabel` | Email | אימייל | Эл. почта |
| `passwordLabel` | Password | סיסמה | Пароль |
| `fullNameLabel` | Full name | שם מלא | Полное имя |
| `ageLabel` | Age | גיל | Возраст |
| `genderLabel` | Gender | מין | Пол |
| `rememberMe` | Remember me | זכור אותי | Запомнить меня |
| `agePlaceholder` | Select age | בחר גיל | Выберите возраст |
| `haveAccount` | Already have an account? | יש לך כבר חשבון? | Уже есть аккаунт? |
| `loginHere` | Log in | התחבר | Войти |
| `tagline` | Adaptive learning that guides you | למידה אדפטיבית שמובילך אותך | Адаптивное обучение, которое ведёт вас |
| `verifyTitle` | Verify your email | אימות מייל | Подтверждение почты |
| `verifyDesc` | We sent a verification code to {email} | שלחנו קוד אימות לכתובת {email} | Мы отправили код на {email} |
| `resendQuestion` | Didn't get a code? | לא קיבלת קוד? | Не получили код? |
| `resend` | Resend | שלח שוב | Отправить снова |
| `resendIn` | Resend in {seconds}s | שלח שוב בעוד {seconds} שנ׳ | Повтор через {seconds}с |
| `verify` | Verify code | אמת קוד | Подтвердить |
| `backToLogin` | Back to login | חזור להתחברות | Назад ко входу |
| `codeValidFor` | Code valid for {minutes} minutes | הקוד תקף ל-{minutes} דקות | Код действует {minutes} минут |
| `copyright` | © 2025 MathGalaxy · All rights reserved | © 2025 MathGalaxy · כל הזכויות שמורות | © 2025 MathGalaxy · Все права защищены |
| `passwordStrengthWeak` | Weak | חלשה | Слабый |
| `passwordStrengthOk` | Good | טובה | Хороший |
| `passwordStrengthStrong` | Strong | חזקה | Надёжный |

> The wordmark "MathGalaxy" is a **brand name** and stays untranslated in all locales (rendered as a
> two-tone lockup, not body text).

**i18n acceptance criteria (must all pass):**
- [ ] No literal user-facing string in JSX — every one resolves via `getAuthStrings(language)[key]`.
- [ ] Screens render correctly in **he (RTL)**, **en (LTR)**, **ru (LTR)** with live switching (no reload).
- [ ] Direction flips from `useLanguage().dir`; layout mirrors via CSS logical properties (§2.2).
- [ ] Longest-string locale (usually ru) never clips, overflows, or breaks the card (§2.3).
- [ ] Email addresses and the OTP stay LTR even inside an RTL layout (bidi isolation).
- [ ] Missing-key safety: English fallback shows readable text, never a blank or a raw key.

**✅ Verified (v3):** Hebrew, English and Russian all retained. Every screen is direction-driven by
`useLanguage().dir` and mirrors via CSS logical properties (one rule, no `if (rtl)`); labels/buttons
are full-width so longer translations reflow vertically and never break the card; email + OTP are
bidi-isolated to stay LTR inside RTL.

---

## §II — Theme System (light + dark)

**Honor the app's existing theme model, don't fork it.** The backend already defines
`ProfileTheme { DARK, LIGHT }` and stores it on `UserProfile`; the frontend exposes it via
`ProfileContext` (`profileData.theme`). The auth screens consume **design tokens only** — no
hardcoded colors anywhere — so flipping the theme re-skins them with zero per-component changes.

**Current gap this spec closes (important for implementation):** today `index.css` only switches
colors via the OS `@media (prefers-color-scheme: dark)` — it is **not wired to `profileData.theme`**.
So a user who picks "Light" in their profile still gets OS colors. The auth work introduces the
correct mechanism that the rest of the app can adopt later:

- A single root attribute **`data-theme="dark" | "light"`** is set from the resolved theme
  (profile theme when logged in; for guests on the auth screens, default **dark** to match the
  "Space Academy" art, with the in-screen `ThemeToggle` allowed to override and persist to
  `localStorage`).
- Tokens are declared **twice**: `:root[data-theme="dark"]` and `:root[data-theme="light"]`. Every
  surface/text/border/glow reads a token, so both modes are guaranteed consistent.
- `prefers-color-scheme` is used **only** as the initial guess before any explicit choice — the
  explicit `data-theme` always wins. This removes the "OS overrides my profile" bug.

**Dark is the hero** (premium space art on a deep navy field). **Light mode** keeps the same
structure but swaps to an airy, high-legibility surface set; the galaxy art uses a lighter,
lower-contrast treatment so the form stays the focus. The full light token set is in §9.

**Theme acceptance criteria:**
- [ ] Both modes use tokens only — `grep` finds no hex literals in the auth components/CSS.
- [ ] Switching `data-theme` (or the profile theme) re-skins all three screens with no layout shift.
- [ ] Contrast passes AA in **both** modes (text, helper text, focus ring, CTA label).
- [ ] The `ThemeToggle` in the top bar reflects and updates the active theme; choice persists.

**✅ Verified (v3):** the auth screens resolve their theme from `profileData.theme` (`ProfileTheme`
DARK/LIGHT) applied as `data-theme` on the root — **not** from `prefers-color-scheme`. The OS
setting is, at most, the first-paint guess before the profile/explicit choice loads; the app theme
always wins. This is the behavior the rest of the app should adopt.

---

## §IV — Logo Placement (single instance, top bar)

**Rule:** the MathGalaxy logo appears **exactly once per screen**, in the **top header bar**, on
**all three screens** (Login, Register, Email Verification).

- **Location:** `AuthTopBar`, pinned to the **`inline-start`** corner (top-right in RTL, top-left in
  LTR) so it sits over the form/header side, **never over the artwork**. The language switcher +
  theme toggle group sits at the opposite (`inline-end`) corner.
- **Not in the artwork:** the galaxy panel carries only the localized **tagline** — no wordmark.
- **Not in the card:** card headers show only the screen title + description — no logo.
- **No duplication:** there is one and only one `BrandLockup` in the DOM per screen.
- **Composition:** `BrandGlyph` (orbital mark) + `Wordmark` — "Math" in `--text-strong`, "Galaxy" in
  `--accent`. Brand colors are theme-independent; the "Math" half flips ink↔white via tokens so it
  stays legible on both light and dark top-bar backgrounds.
- **a11y:** the lockup is a single focusable home link with an accessible name "MathGalaxy"; the
  decorative glyph is `aria-hidden`.

---

## §III — Age Field (1st grade → university and beyond)

The platform serves grade-1 children **today** and must serve **university students** next, so the
age control cannot be a child-only picker. Requirements:

- **Range:** supports **4 → 120** (backend already validates 1–120). Covers kindergarten through
  adult/university/lifelong learners.
- **Control:** a styled **`Select`** grouped for fast scanning, backed by a native `<select>` for
  accessibility and mobile ergonomics:
  - Group **"Kids"** 4–11, **"Teens"** 12–17, **"Adults"** 18–25, then **"26+"** in steps that stay
    short (single scroll), ending at 120. (Exact bucketing is cosmetic; the value submitted is the
    integer age.)
  - Alternatively a **number stepper** with min=4/max=120 — equally acceptable; the Select is the
    default recommendation because it avoids free-text typos and works identically in RTL/LTR.
- **No age-based restyling.** Like gender, age never changes colors, theme, or layout — a 7-year-old
  and a 22-year-old see the identical premium UI. Age only feeds the adaptive engine downstream.
- **i18n:** label `ageLabel`, placeholder `agePlaceholder`; group headers localized
  (`ageGroupKids/Teens/Adults/Older`). Numerals render in the locale via `Intl` where appropriate.
- **a11y:** real `<label for>`, keyboard-operable, screen-reader announces the selected age.

> This document is the source of truth for the three auth screens. It is intentionally
> framework-agnostic (no React code). It defines the tokens, layout, and behavior that the
> implementation will consume. Everything here is RTL/LTR-symmetric and dark-mode-first.

---

## 1. Visual Concept

**The idea in one line:** *A calm window into deep space, with a focused glass console where you sign in.*

MathGalaxy's auth is the first impression for a 1st-grader and a university student alike, so it
must read as **premium and timeless**, never childish. We achieve "friendly but serious" through:

- **Atmosphere, not characters.** The space theme lives entirely in the background artwork —
  galaxy, nebula, planet rim, faint starfield, an occasional comet streak. **No mascots, no
  astronauts, no robots, no cartoons.** This is what keeps it credible for adult learners while
  still feeling wondrous to a child.
- **A single point of focus.** One glass-morphism card floats over the artwork. Everything the
  user must do is inside that card; the artwork never competes with it.
- **Quiet motion.** Subtle, slow parallax on the starfield and a gentle glow pulse on the primary
  button. Motion is ambient, never demanding. Respects `prefers-reduced-motion`.
- **Trust signals.** Clear hierarchy, generous spacing, real focus states, a visible language
  switcher, and a footer with copyright — the small cues that say "this is a real product."

**Emotional target:** *modern · premium · clean · friendly · trustworthy · educational.*
**Explicitly avoided:** *childish · cartoonish · toy-like · overloaded.*

This concept matches the reference mockup (dark navy field, purple-accented galaxy art on one
half, glass console on the other, MathGalaxy wordmark with the geometric orbital glyph).

---

## 2. Detailed Layout

### 2.1 The shared shell (all three screens)

All three screens share **one** layout component (`AuthLayout`) so they feel like one place. It is a
**split canvas**:

```
┌───────────────────────────────────────────────────────────────┐
│  [◇ MathGalaxy]                          [🌐 Language ▼] [☀/🌙] │  ← top bar: logo (single) + controls
│                                                               │
│   ┌─────────────────────────┐   ┌──────────────────────────┐  │
│   │                         │   │                          │  │
│   │      ARTWORK PANE       │   │       CONSOLE PANE       │  │
│   │   (galaxy / nebula /    │   │   (the glass card with   │  │
│   │    planet / starfield   │   │    the actual form)      │  │
│   │    + MathGalaxy lockup) │   │                          │  │
│   │                         │   │                          │  │
│   └─────────────────────────┘   └──────────────────────────┘  │
│                                                               │
│              © 2025 MathGalaxy · All rights reserved          │  ← footer
└───────────────────────────────────────────────────────────────┘
```

- **Desktop / laptop (≥ 1024px):** 50 / 50 split. Artwork pane and console pane each take half.
  The card is vertically centered in its pane with comfortable padding.
- **Tablet (640–1023px):** Artwork collapses to a **slim banner strip** across the top (~30vh)
  carrying the galaxy + tagline; the form card sits below, centered, max-width 480px.
- **Mobile (< 640px):** Single column. The artwork becomes a **full-bleed background** behind a
  darkening scrim, and the card floats on top, full-width minus 20px gutters. The **logo stays in
  the top bar** on every viewport (single instance) — it is never moved into the card.

### 2.2 RTL / LTR symmetry (critical)

The split is **direction-aware**, not hard-coded left/right:

- The brief shows Hebrew (RTL): **artwork on the LEFT, form on the RIGHT**.
- In LTR (English/Russian) the panes mirror: **artwork on the RIGHT, form on the LEFT** — or we
  keep artwork-start / form-end consistently. **Decision: artwork is always on the `inline-start`
  side, form on `inline-end`.** In RTL `inline-start` = right... so to match the mockup exactly
  (art left in Hebrew) the artwork is on the **`inline-end`** side. We implement with logical CSS
  (`inset-inline`, `margin-inline`, `padding-inline`, flex order via `flex-direction: row` + DOM
  order) so a single rule produces a correct mirror in both directions — no `if (rtl)` branches.

> Implementation note for later: use CSS logical properties everywhere (`padding-inline-start`,
> `border-start-start-radius`, `text-align: start`). The top bar's language switcher sits at
> `inset-inline-start`, the theme toggle at `inset-inline-end`, so they swap automatically.

### 2.3 No-break guarantee (multi-language)

German-length or Russian-length labels must never break the layout:

- Inputs and buttons are **full-width of the card**, so longer labels reflow above the field, never
  beside it.
- Button labels are centered with `white-space: nowrap` **only** on short CTAs; longer secondary
  links wrap naturally on two lines with balanced wrapping.
- The card has a fixed `max-width` (see §5) and grows in **height**, never width, as fields are
  added (Register is taller than Login — that's expected and handled by vertical scroll on short
  viewports).

---

## 3. Color Usage

All values are the approved tokens. Below is **where each one is used** (the part that prevents the
fractured-palette problem the current app has).

| Token | Hex | Role in auth screens |
|---|---|---|
| `--bg` Primary BG | `#0B1020` | The deep-space base behind everything; the mobile scrim. |
| `--surface-card` Card BG | `#131A2E` | The glass console card body. |
| `--surface-2` Secondary surface | `#1A2340` | Input fields, OTP boxes, segmented gender control, the "or" divider chips. |
| `--accent` Primary | `#7C4DFF` | Primary CTA gradient start, focus ring, logo glyph, links, active states. **The one hero color.** |
| `--accent-2` Secondary | `#3B82F6` | Primary CTA gradient end, secondary glows, info accents. Pairs with `--accent` only in gradients. |
| `--success` | `#22C55E` | Valid-field check, "code verified" state. |
| `--warning` | `#F59E0B` | Soft inline warnings (e.g. "code expires in 10 min" emphasis). |
| `--danger` | `#EF4444` | Validation errors, wrong-code shake border, destructive feedback. |
| `--stars` | `#FBBF24` | Reserved — not used on auth except the faint starfield highlights. Keeps brand continuity with the in-app stars currency. |

**Rules of use:**
- **One accent identity.** `--accent` (`#7C4DFF`) is the brand. `--accent-2` only appears *with* it
  inside the CTA gradient `linear-gradient(135deg, #7C4DFF → #3B82F6)` and in ambient glows. We do
  **not** introduce a third blue (this kills the current `#007bff` vs `#aa3bff` conflict).
- **Glow is subtle.** Glows are low-opacity (`box-shadow: 0 0 40px rgba(124,77,255,.25)`), used on
  the CTA, focused inputs, and the email icon halo. Never neon, never on text.
- **Gender selection does NOT recolor the UI.** Both options use the same `--surface-2` resting
  state and the same `--accent` selected state. Identical for every user — per the brief.
- **Status colors are feedback-only.** Success/warning/danger appear on validation and never as
  decoration, so the screen stays calm.

**Contrast (accessibility):** Body text `#C7CDDB` on `#131A2E` ≈ 9:1; muted helper text `#8B93A7`
on card ≈ 4.7:1 (passes AA for ≥14px). CTA white text on the purple gradient ≥ 4.5:1. All
interactive elements get a visible `--accent` focus ring at ≥ 3:1 against the surface.

---

## 4. Typography System

**Type family:** A clean geometric-humanist sans that ships multilingual Latin + Hebrew + Cyrillic.
**Primary: `Inter`** (Latin/Cyrillic) with **`Heebo`** for Hebrew, or a single super-family
(**`Rubik`** covers all three and reads friendly-but-serious — recommended for one-font simplicity).
Numerals use **tabular figures** for the OTP boxes and any numeric UI.

```
Recommendation: Rubik (he/en/ru in one family) → fewest moving parts, consistent across languages.
Fallback stack: Rubik, "Heebo", Inter, system-ui, "Segoe UI", Roboto, sans-serif
```

**Scale (rem @ 16px root), responsive-clamped:**

| Token | Size | Weight | Use |
|---|---|---|---|
| `display` | `clamp(28px, 4vw, 34px)` | 700 | Card title ("ברוך שובך!", "צור חשבון חדש", "אימות מייל") |
| `h2` | `22px` | 600 | Secondary headings if needed |
| `body-lg` | `16px` | 400 | Descriptions under the title |
| `body` | `15px` | 400 | Input values, general text |
| `label` | `13px` | 600 | Field labels (Email, Password, Gender…) |
| `caption` | `13px` | 400 | Helper text, footer, "code expires in 10 min" |
| `link` | `14px` | 600 | "Register", "Login", "Resend code" |

- **Line-height:** 1.2 for display, 1.5 for body/caption.
- **Letter-spacing:** slight negative (`-0.01em`) on display only; Hebrew gets `0` (never negative-track Hebrew).
- **Wordmark:** "Math" in `--text-strong` white, "Galaxy" in `--accent` — set in the same weight
  (700) so it reads as one word with a color shift, matching the mockup.

---

## 5. Component Hierarchy

Reusable pieces the three screens are assembled from (these become the first entries of the design
system, reused far beyond auth):

```
AuthLayout                         ← split canvas, direction-aware, owns artwork + footer
├── AuthTopBar                     ← the ONLY place the logo lives (all 3 screens)
│   ├── BrandLockup                (logo, single instance — inline-start of the bar)
│   │   ├── BrandGlyph             (orbital geometric mark)
│   │   └── Wordmark               ("Math" white/ink + "Galaxy" accent)
│   └── Controls                   (inline-end of the bar)
│       ├── LanguageSwitcher       (🌐 + current language, dropdown he/en/ru)
│       └── ThemeToggle            (☀/🌙)
├── AuthArtwork                    (galaxy/nebula/planet bg + tagline only; NO logo; decorative, aria-hidden)
│   └── Tagline                    (localized strap line under the galaxy)
└── AuthCard                       ← the glass console (the only thing that changes per screen)
    ├── CardHeader (title + description — NO logo here)
    └── [screen-specific body]

Shared form atoms (design-system level):
- Field            (label + control + helper/error slot, vertical)
- TextInput        (with leading icon slot; email, name)
- PasswordInput    (TextInput + trailing show/hide toggle)
- Select           (age; native-backed for a11y, styled)
- SegmentedControl (gender: two equal segments, no color identity change)
- Checkbox         (remember me — UI only, no backend behavior yet; §6)
- Button           (variant: primary=gradient, secondary=outline, ghost=link)
- OtpInput         (6 single-char boxes, auto-advance, paste-aware)
- Link             (accent, underline on hover/focus)
- Alert            (inline success/warning/danger)
```

### 5.1 Login card body
```
CardHeader:  "ברוך שובך!" / "Welcome back!"   + "התחבר כדי להמשיך"
Field: Email        → TextInput (leading ✉ icon)
Field: Password     → PasswordInput (leading 🔒, trailing 👁 toggle)
Row:   Checkbox "Remember me"        (start-aligned; UI-only placeholder)
Button: primary "Login"  (full-width, gradient, glow)
Link (centered): "Don't have an account? Register"
```
> **No "Continue with Google", no "or" divider** — removed in v2. The CTA is the single primary
> action; the register link follows directly below it.

### 5.2 Register card body
```
CardHeader:  "צור חשבון חדש" / "Create a new account"  + "הצטרף למיליוני לומדים"
Field: Full Name    → TextInput (leading 👤)
Field: Email        → TextInput (leading ✉)
Field: Password     → PasswordInput (leading 🔒, toggle, + strength hint)
Field: Age          → Select (leading 📅, grouped Kids/Teens/Adults/Older, range 4–120; §III)
Field: Gender       → SegmentedControl [Female | Male]  (identical styling both)
Button: primary "Register" (full-width)
Link (centered): "Already have an account? Login"
```
> Register reuses the **exact** AuthLayout + AuthCard. Only the body fields differ. The card simply
> grows taller; on short viewports the card scrolls internally, the artwork stays fixed.

### 5.3 Email Verification card body
```
EmailIcon (large, with soft --accent halo + success check badge)
Title:       "אימות מייל" / "Verify your email"
Description: "A verification code was sent to {email}"   (email in --text-strong)
OtpInput:    6 boxes (tabular figures, auto-advance, paste fills all)
Link:        "Didn't get a code? Resend"    (disabled w/ countdown after click)
Button:      primary "Verify"  (full-width; disabled until 6 digits entered)
Link:        "← Back to login"
Caption:     "Code valid for 10 minutes"   (warning-tinted emphasis on the number)
```

---

## 6. User Flow

```
                         ┌─────────────┐
                         │   LOGIN     │◄───────────────────────────┐
                         └──────┬──────┘                            │
              ┌─────────────────┼───────────────────┐              │
        valid creds        invalid creds       "Register" link     │
              │                 │                   │              │
              ▼                 ▼                   ▼              │
        app (dashboard)   inline error +      ┌──────────┐         │
                          shake on card       │ REGISTER │         │
                                              └────┬─────┘         │
                                       submit valid│  "Login" link─┘
                                                   ▼
                                          ┌─────────────────┐
                                          │ EMAIL VERIFY    │
                                          │ (code sent)     │
                                          └───┬─────────┬───┘
                                  correct code│         │"Back to login"──┐
                                              ▼         │  wrong code:     │
                                        app (dashboard) │  shake + danger  │
                                                        └──────────────────┘
                                                            ▲
                                                  "Resend" → new code, countdown
```

**States every screen must show:**
- **Idle** — clean, primary CTA enabled only when the form is minimally valid.
- **Loading** — CTA shows a spinner + disabled; inputs locked. (Network in flight.)
- **Field error** — danger border + helper message under the offending field; the rest stay calm.
- **Form error** — a single inline `Alert` at the top of the card body (e.g. "Email or password is
  incorrect"); subtle horizontal shake of the card (respects reduced-motion).
- **Success** — brief success state (check on the CTA) before navigation.
- **Remember Me (UI-only)** — the checkbox toggles and is keyboard/screen-reader operable, but has
  **no backend effect** in this build (session TTL unchanged). It is a visual placeholder pending a
  future backend task; do not gate any behavior on it.
- **Verification specifics** — OTP auto-advances per digit, backspace moves back, paste of a 6-digit
  code fills all boxes; "Verify" enables at 6 digits; "Resend" disables with a visible 30–60s
  countdown; expiry caption reflects the 10-minute window.

---

## 7. High-Fidelity Wireframes (ASCII)

> The pixel-accurate reference is your attached mockup; these wireframes lock the structure,
> spacing rhythm, and RTL behavior the implementation must hit. Shown in RTL (Hebrew) to match the
> mockup; LTR is the mirror image.

### 7.1 Login — Desktop (RTL)
```
╔════════════════════════════════════════════════════════════════════════╗
║ [🌐 עברית ▾] [ 🌙 ]                              [◇ MathGalaxy]  ║ ← logo @ inline-start (form side)
║                                                                        ║
║   ·  . ✦   *  .       ☄                ┌──────────────────────────┐   ║
║      *   .    ✦   .                    │                            │   ║
║   .      ╭───────╮       .             │        ברוך שובך!          │   ║ ← card header: title only
║      .   │ GALAXY│   .       *         │     התחבר כדי להמשיך       │   ║   (no logo in card)
║      .   ╰───────╯  .        ✦         │                            │   ║
║   (galaxy + tagline only, NO logo)     │  אימייל                    │   ║
║   למידה אדפטיבית שמובילך אותך          │ ┌────────────────────────┐ │   ║
║      .   *   .   ✦    .   *            │ │ ✉  example@email.com   │ │   ║
║   ●───── planet rim ─────              │ └────────────────────────┘ │   ║
║      .       .     ✦                   │  סיסמה                     │   ║
║                                        │ ┌────────────────────────┐ │   ║
║                                        │ │ 🔒  ••••••••       👁  │ │   ║
║                                        │ └────────────────────────┘ │   ║
║                                        │            זכור אותי ☐     │   ║
║                                        │ ┌────────────────────────┐ │   ║
║                                        │ │       התחבר  (CTA)     │ │   ║
║                                        │ └────────────────────────┘ │   ║
║                                        │                            │   ║
║                                        │   אין לך חשבון? הרשמה       │   ║
║                                        └──────────────────────────┘   ║
║                    © 2025 MathGalaxy · כל הזכויות שמורות               ║
╚════════════════════════════════════════════════════════════════════════╝
```

### 7.2 Register — Desktop (RTL)  · (logo lives in the top bar, not shown in this card-only crop)
```
        ┌──────────────────────────┐
        │       צור חשבון חדש        │
        │   הצטרף למיליוני לומדים    │
        │  שם מלא                    │
        │ ┌────────────────────────┐ │
        │ │ 👤  הכנס שם מלא         │ │
        │ └────────────────────────┘ │
        │  אימייל                    │
        │ ┌────────────────────────┐ │
        │ │ ✉  example@email.com   │ │
        │ └────────────────────────┘ │
        │  סיסמה                     │
        │ ┌────────────────────────┐ │
        │ │ 🔒  ••••••••       👁  │ │
        │ └────────────────────────┘ │
        │  גיל                       │
        │ ┌────────────────────────┐ │
        │ │ 📅  בחר גיל          ▾ │ │   ← grouped 4–120 (ילדים/נוער/מבוגרים/+26)
        │ └────────────────────────┘ │
        │  מין                       │
        │ ┌───────────┬────────────┐ │   ← SegmentedControl, both identical
        │ │  ♀ נקבה   │   זכר ♂    │ │      (selected = --accent fill, no per-gender color)
        │ └───────────┴────────────┘ │
        │ ┌────────────────────────┐ │
        │ │      הרשמה  (CTA)      │ │
        │ └────────────────────────┘ │
        │  יש לך כבר חשבון? התחבר     │
        └──────────────────────────┘
```

### 7.3 Email Verification — Desktop (RTL)  · (logo lives in the top bar, not shown in this card-only crop)
```
        ┌──────────────────────────┐
        │            ╭────╮          │
        │            │ ✉✓ │   ← large icon + halo + success check
        │            ╰────╯          │
        │         אימות מייל         │
        │  שלחנו קוד אימות לכתובת    │
        │  example@email.com        │
        │                            │
        │  ┌─┐┌─┐┌─┐┌─┐┌─┐┌─┐        │   ← 6 OTP boxes, tabular figures
        │  │1││2││3││4││5││6│        │
        │  └─┘└─┘└─┘└─┘└─┘└─┘        │
        │       לא קיבלת קוד? שלח שוב │
        │ ┌────────────────────────┐ │
        │ │      אמת קוד  (CTA)    │ │
        │ └────────────────────────┘ │
        │      → חזור להתחברות        │
        │   הקוד תקף ל-10 דקות        │   ← number in --warning emphasis
        └──────────────────────────┘
```

### 7.4 Mobile (all three, single column)
```
┌─────────────────────┐
│ [◇ MathGalaxy]  [🌐 🌙] │ ← logo stays in top bar (single instance, all viewports)
│  ░░ galaxy bg ░░░░  │   ← full-bleed art + scrim (tagline only, no logo)
│ ┌─────────────────┐ │
│ │  ברוך שובך!     │ │   ← card header: title only
│ │  [ ✉ email    ] │ │
│ │  [ 🔒 pass  👁 ]│ │
│ │  זכור אותי ☐    │ │
│ │  [   התחבר    ] │ │
│ │  אין חשבון? הרשמה│ │
│ └─────────────────┘ │
│  © 2025 MathGalaxy  │
└─────────────────────┘
```

---

## 8. Resolved Decisions (v2)

1. **Google / OAuth — REMOVED.** No "Continue with Google", no "or" divider, no OAuthButton in the
   build. CTA is the single primary action. (Can be reintroduced later if the backend adds OAuth.)
2. **Remember Me — UI ONLY.** The checkbox renders and is operable, but **no backend wiring yet**:
   it does not change session TTL (backend stays at its current 1-day token). Treated as a visual
   placeholder; flagged in §6 states so it isn't mistaken for functional. Wire to a longer TTL in a
   future backend task.
3. **Theme — both modes, app-driven.** Light + dark token sets defined (§9), driven by the existing
   `ProfileTheme` via `data-theme` (§II). Dark is default for guests on auth.
4. **Age — wide range.** Grouped Select, 4–120, no restyling (§III).

**Remaining (cosmetic, safe to decide at implementation):**
- **Font:** `Rubik` (one family, he/en/ru) recommended. Falls back to `Heebo`/`Inter`/system. Say the
  word if you have a brand font.

---

## 9. Token Sheet (feeds implementation — `tokens.css`)

Brand hues are theme-independent; **surfaces, text, borders, and glows are defined per theme** under
`data-theme`. Components reference only these variables — never a raw hex.

```
/* ---- Brand (same in both themes) ---- */
--accent:          #7C4DFF;   /* primary brand             */
--accent-2:        #3B82F6;   /* secondary (gradient only) */
--success:         #22C55E;
--warning:         #F59E0B;
--danger:          #EF4444;
--stars:           #FBBF24;
--cta-gradient:    linear-gradient(135deg, #7C4DFF 0%, #3B82F6 100%);
--radius:          16px;  --radius-sm: 10px;  --radius-pill: 999px;

/* ============ DARK (default) — :root[data-theme="dark"] ============ */
--bg:              #0B1020;   /* primary background        */
--surface-card:    #131A2E;   /* card (glass over art)     */
--surface-2:       #1A2340;   /* inputs, OTP, segments     */
--text-strong:     #F4F6FB;   /* titles, values            */
--text:            #C7CDDB;   /* body                      */
--text-muted:      #8B93A7;   /* helper, captions          */
--border:          rgba(255,255,255,.08);
--border-strong:   rgba(255,255,255,.16);
--ring:            rgba(124,77,255,.55);             /* focus ring        */
--glow-accent:     0 0 40px rgba(124,77,255,.25);
--art-scrim:       rgba(11,16,32,.55);               /* mobile bg scrim   */
--card-blur:       saturate(140%) blur(18px);        /* glass effect      */

/* ============ LIGHT — :root[data-theme="light"] ============ */
--bg:              #F4F6FC;   /* airy off-white field      */
--surface-card:    #FFFFFF;   /* solid card                */
--surface-2:       #EEF1FA;   /* inputs, OTP, segments     */
--text-strong:     #0B1020;   /* titles, values            */
--text:            #2C3450;   /* body                      */
--text-muted:      #5B6480;   /* helper, captions          */
--border:          rgba(11,16,32,.10);
--border-strong:   rgba(11,16,32,.18);
--ring:            rgba(124,77,255,.45);
--glow-accent:     0 0 32px rgba(124,77,255,.18);
--art-scrim:       rgba(244,246,252,.45);
--card-blur:       saturate(120%) blur(10px);
/* Light mode: galaxy art uses a lighter, lower-contrast treatment so the form stays the focus. */

/* Spacing (4-pt scale) */
--space-1: 4px;  --space-2: 8px;  --space-3: 12px;  --space-4: 16px;
--space-5: 20px; --space-6: 24px; --space-8: 32px;  --space-10: 40px;

/* Sizing */
--card-max:        440px;     /* console max-width         */
--control-h:       48px;      /* inputs / buttons height   */
--otp-box:         52px;

/* Type */
--font-sans:       Rubik, "Heebo", Inter, system-ui, "Segoe UI", Roboto, sans-serif;

/* Motion */
--ease:            cubic-bezier(.2,.8,.2,1);
--dur-fast:        140ms;
--dur:             240ms;
```

---

## 10. Accessibility Checklist (must pass at implementation)

- [ ] All inputs have associated `<label>` (not placeholder-only).
- [ ] Visible focus ring (`--ring`) on every interactive element, ≥ 3:1 contrast.
- [ ] Full keyboard path: Tab order top→bottom, Enter submits, OTP arrow-key nav + backspace.
- [ ] Show/hide password toggle is a real `<button>` with `aria-pressed` + accessible name.
- [ ] Gender segmented control is a `radiogroup`; age select is a native `<select>` under the hood.
- [ ] Errors announced via `aria-live="polite"`; field errors linked with `aria-describedby`.
- [ ] Decorative artwork is `aria-hidden`; meaningful icons have labels.
- [ ] Color is never the only signal (errors also carry text + icon).
- [ ] `prefers-reduced-motion` disables parallax, glow pulse, and shake.
- [ ] RTL/LTR verified: no clipped text, mirrored layout, math/email kept LTR where needed.
- [ ] Min 44×44px touch targets on mobile.
```
