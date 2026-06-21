# i18n — Adding language support to new pages

How to add multi-language support (Hebrew / Russian / English, more later) to a **new
page** in a consistent way. The shared infrastructure lives in
[`languages.js`](./languages.js) and [`useLanguage.js`](./useLanguage.js).

> Adding a new language later = one block per strings dictionary + one entry in
> `LANG_CODE` (and the backend enum). No other code changes.

---

## Rules

### 1. Always use `useLanguage`
Every page reads the active language through the single hook — never read
`profileData.language` directly, never re-derive it.

```js
const { language, dir, locale, isRtl } = useLanguage();
// language: "he" | "en" | "ru"   dir: "rtl" | "ltr"   locale: "he-IL" | ...
```

It already resolves the right source: logged-in user → profile (server), guest →
`localStorage`. You don't need to know which.

### 2. Put texts in a per-feature strings file
One dictionary per feature/area, next to the feature:

```
components/<Feature>/<feature>Strings.js   →  getXStrings(language)
```

Follow the existing pattern (`authStrings.js`, `navStrings.js`, `adminStrings.js`).
**Do not** create one giant global strings file.

- **New file** — a new feature/area (its own screen + its own set of strings). This
  is the default.
- **Add to an existing file** — only when the page belongs to the same logical area
  that already has a dictionary (e.g. another admin screen → add to `adminStrings`).
- Rule of thumb: share by **logical area**, not by convenience.

### 3. Use `getStrings`
Build a dictionary keyed by ISO code with an **English base** (fallback), exposed via
a thin getter:

```js
import { getStrings } from "../../i18n/languages.js";

export const FEATURE_STRINGS = {
    en: { title: "Reports", colDate: "Date" },
    he: { title: "דוחות",  colDate: "תאריך" },
    ru: { title: "Отчёты", colDate: "Дата" },
};

export const getFeatureStrings = (language) => getStrings(FEATURE_STRINGS, language);
```

`getStrings` merges the English base with the chosen language, so a missing key falls
back to readable English instead of rendering blank. For interpolation use
`format("Page {n}", { n })` from `languages.js`.

### 4. Never re-define the shared primitives
Always `import` from `i18n/languages.js`. Do **not** re-declare:

`LANG_CODE` · `DEFAULT_LANGUAGE` · `RTL_LANGUAGES` · `LOCALE_BY_LANGUAGE` ·
`isRtl` · `localeFor` · `getStrings`

Also: do **not** hand-write `LANG_CODE[...] || 'he'` — use `useLanguage()`.

### 5. No hardcoded texts in new pages
Every visible string (placeholder, button, title, **error message**) goes through
`t.key`. No literal text in JSX.

### 6. Don't put a `LanguageSwitcher` on every page
A new page only **consumes** the language (`useLanguage`); it does **not** render a
switcher.

### 7. Where the `LanguageSwitcher` actually appears
- **Navbar** — for logged-in users (hidden on the active exercise screen).
- **Login / Register** — for guests.
- **Profile Settings** — the persistent preference.

### 8. `dir` and `locale`
- **`dir`** is owned by the **layout / page root** — the page's root container gets
  `dir={dir}`. If the page renders under a layout that already sets `dir` (e.g.
  `AdminLayout`), **don't** repeat it.
- **Presentational components** never set `dir` themselves — they receive
  `dir` / `locale` / `t` via props and stay pure.
- **`locale`** for every date/number: `new Date(x).toLocaleString(locale)` — never
  hardcode `"he-IL"`.
- For layout that flips: use `textAlign: "start"` and `marginInlineStart` instead of
  `right` / `left`.

---

## 9. Example (new page)

```jsx
// pages/Reports/Reports.jsx  — smart page
import { useLanguage } from "../../i18n/useLanguage.js";
import { getReportsStrings } from "../../components/Reports/reportsStrings.js";
import { ReportsTable } from "../../components/Reports/ReportsTable.jsx";

export const Reports = () => {
    const { language, dir, locale } = useLanguage();
    const t = getReportsStrings(language);
    // ... data fetching / state ...
    return (
        <div dir={dir} style={{ padding: 24 }}>
            <h1>{t.title}</h1>
            <ReportsTable rows={rows} t={t} locale={locale} /> {/* dumb: props only */}
        </div>
    );
};
```

```js
// components/Reports/reportsStrings.js
import { getStrings } from "../../i18n/languages.js";

export const REPORTS_STRINGS = {
    en: { title: "Reports", colDate: "Date" },
    he: { title: "דוחות",  colDate: "תאריך" },
    ru: { title: "Отчёты", colDate: "Дата" },
};

export const getReportsStrings = (language) => getStrings(REPORTS_STRINGS, language);
```

> If the page sits under a layout that already provides `dir`, drop `dir={dir}` from
> the root.

---

## 10. Developer checklist (new page)

- [ ] `const { language, dir, locale } = useLanguage();` — no manual language reads.
- [ ] A `<feature>Strings.js` with `en/he/ru`, English base, exposed via
      `getXStrings` built on `getStrings`.
- [ ] **Zero** hardcoded text in JSX — everything is `t.key` (including errors).
- [ ] `dir` on the page root (unless a layout already provides it).
- [ ] Dates/numbers use `locale`; layout uses `start` / `inline`, not `right` / `left`.
- [ ] Presentational components receive `t` / `locale` / `dir` via props; stay pure.
- [ ] **No** `LanguageSwitcher` in the page (only Navbar / Login / Register / Settings).
- [ ] **No** re-definition of any i18n primitive — always import from
      `i18n/languages.js`.
- [ ] A future language = one block per dictionary + one `LANG_CODE`/enum entry; no
      other code changes.
