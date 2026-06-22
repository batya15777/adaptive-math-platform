// Localization helpers for data the backend sends in English (topic names + badge names).
// Single, tidy place for the fallback mapping — components call these instead of
// embedding any English-detection logic. If a value isn't recognized, the original
// backend text is returned unchanged (graceful fallback, never a blank).

const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);

// "Add" / "Mult" / "Fractions" → localized topic name. `t` is the dashboard strings dict.
export const topicName = (name, t) => {
    const n = (name || '').toLowerCase();
    const m = t.topicNames || {};
    if (n.includes('add')) return m.add;
    if (n.includes('sub')) return m.sub;
    if (n.includes('mult')) return m.mult;
    if (n.includes('div')) return m.div;
    if (n.includes('frac')) return m.fractions;
    if (n.includes('dec')) return m.decimals;
    return cap(name); // unknown topic → show the backend name as-is
};

// Backend badge name → a normalized key into t.badgeNames.
const badgeKey = (name) => {
    const n = (name || '').toLowerCase();
    if (n.includes('first') || n.includes('step')) return 'firstSteps';
    if (n.includes('collector') || n.includes('star collect')) return 'starCollector';
    if (n.includes('persist')) return 'persistent';
    if (n.includes('explor')) return 'explorer';
    if (n.includes('sharp') || n.includes('shooter') || n.includes('marks')) return 'sharpShooter';
    if (n.includes('rising')) return 'risingStar';
    return null;
};

// Localized, gender-aware badge display name. `gk` is 'male' | 'female' | 'neutral'.
// Falls back to the backend name when the badge isn't in the translation map.
export const badgeName = (badge, t, gk = 'neutral') => {
    const key = badgeKey(badge?.name);
    const entry = key && t.badgeNames ? t.badgeNames[key] : null;
    if (!entry) return badge?.name || '';
    return entry[gk] ?? entry.neutral ?? (badge?.name || '');
};
