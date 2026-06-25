// Client-only accessibility preferences (localStorage). Applied as data-attributes on <html>;
// the global CSS in styles/accessibility.css reacts to them, SCOPED to the student .mg-space UI
// (admin / auth screens are untouched). No server / API / DB involved.
const KEY = 'mg.a11y';
const TEXT_VALUES = ['sm', 'md', 'lg'];

export const A11Y_DEFAULTS = { text: 'md', contrast: false, motion: false, font: false };

export const loadA11y = () => {
    try {
        const saved = JSON.parse(localStorage.getItem(KEY) || '{}');
        return {
            text: TEXT_VALUES.includes(saved.text) ? saved.text : A11Y_DEFAULTS.text,
            contrast: !!saved.contrast,
            motion: !!saved.motion,
            font: !!saved.font,
        };
    } catch {
        return { ...A11Y_DEFAULTS };
    }
};

export const saveA11y = (s) => {
    try { localStorage.setItem(KEY, JSON.stringify(s)); } catch { /* storage unavailable — ignore */ }
};

// Reflect settings onto <html> so the global CSS can react. Data-attributes (not classes) keep
// this independent of any framework class already on the root element.
export const applyA11y = (s) => {
    const el = document.documentElement;
    el.setAttribute('data-a11y-text', TEXT_VALUES.includes(s.text) ? s.text : 'md');
    el.toggleAttribute('data-a11y-contrast', !!s.contrast);
    el.toggleAttribute('data-a11y-motion', !!s.motion);
    el.toggleAttribute('data-a11y-font', !!s.font);
};
