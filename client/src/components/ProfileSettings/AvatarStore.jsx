import { useState } from 'react';
import { format } from '../../i18n/languages.js';
import { AVATAR_CATALOG } from './avatarCatalog.js';
import { AvatarBubble } from './AvatarBubble.jsx';
import './AvatarStore.css';

// Avatar store overlay. Pure-ish: receives the current selection + owned ids + the star
// balance, and calls back to the parent (which owns the real persistence). Gender-aware
// default tab, but never blocks browsing "all".
//   gk        'male' | 'female' | 'neutral'
//   stars     current star balance (read-only)
//   selectedId / ownedIds   current selection + locally-owned ids
//   onSelect(entry) / onBuy(entry) / onClose
//   t, g, locale
export function AvatarStore({ gk = 'neutral', stars = 0, selectedId, ownedIds = [], onSelect, onBuy, onClose, t, g, locale, initialTab = 'recommended' }) {
    const [tab, setTab] = useState(initialTab);

    const isOwned = (a) => a.price === 0 || ownedIds.includes(a.id);
    const isSelected = (a) => a.id === selectedId;

    const TABS = [
        { key: 'recommended', label: t.tabRecommended },
        { key: 'girls', label: t.tabGirls },
        { key: 'boys', label: t.tabBoys },
        { key: 'all', label: t.tabAll },
        { key: 'mine', label: t.tabMine },
    ];

    const matchesTab = (a) => {
        switch (tab) {
            case 'girls': return a.gender === 'female' || a.gender === 'neutral';
            case 'boys': return a.gender === 'male' || a.gender === 'neutral';
            case 'mine': return isOwned(a);
            case 'recommended': return gk === 'neutral' ? true : (a.gender === gk || a.gender === 'neutral');
            default: return true;
        }
    };
    const list = AVATAR_CATALOG.filter(matchesTab);

    const priceLabel = (a) => (a.price === 0 ? t.free : `${a.price.toLocaleString(locale)} ⭐`);

    return (
        <div className="as-overlay" role="dialog" aria-modal="true" aria-label={g(t.storeTitle)} onClick={onClose}>
            <div className="as-panel" onClick={(e) => e.stopPropagation()}>
                <div className="as-head">
                    <div>
                        <h2 className="as-title">{g(t.storeTitle)}</h2>
                        <p className="as-stars">⭐ {format(t.starsAvailable, { stars: stars.toLocaleString(locale) })}</p>
                    </div>
                    <button type="button" className="as-x" onClick={onClose} aria-label={t.close}>✕</button>
                </div>

                <div className="as-tabs">
                    {TABS.map(tb => (
                        <button key={tb.key} type="button" className={`as-tab${tab === tb.key ? ' is-active' : ''}`} onClick={() => setTab(tb.key)}>
                            {tb.label}
                        </button>
                    ))}
                </div>

                <div className="as-grid">
                    {list.length === 0 && <p className="as-empty">{t.storeEmpty}</p>}
                    {list.map(a => {
                        const owned = isOwned(a);
                        const selected = isSelected(a);
                        const canBuy = stars >= a.price;
                        const name = t.avatarNames?.[a.nameKey] || a.nameKey;
                        return (
                            <div key={a.id} className={`as-card rarity-${a.rarity}${selected ? ' is-selected' : ''}`}>
                                {selected ? <span className="as-tag as-tag--selected">{t.selected}</span>
                                    : a.price === 0 ? <span className="as-tag as-tag--free">{t.free}</span> : null}

                                <div className="as-bubble-wrap">
                                    <AvatarBubble entry={a} size={72} alt={name} />
                                    {!owned && <span className="as-lock" aria-hidden="true">🔒</span>}
                                </div>

                                <div className="as-name">{name}</div>
                                <div className={`as-price${a.price === 0 ? ' is-free' : ''}`}>{priceLabel(a)}</div>

                                {selected ? (
                                    <button type="button" className="as-btn as-btn--selected" disabled>{t.selected}</button>
                                ) : owned ? (
                                    <button type="button" className="as-btn as-btn--select" onClick={() => onSelect(a)}>{g(t.select)}</button>
                                ) : canBuy ? (
                                    <button type="button" className="as-btn as-btn--buy" onClick={() => onBuy(a)}>{g(t.buy)} · {a.price} ⭐</button>
                                ) : (
                                    <button type="button" className="as-btn as-btn--need" disabled>{format(t.needStars, { n: (a.price - stars).toLocaleString(locale) })}</button>
                                )}
                            </div>
                        );
                    })}
                </div>

                <div className="as-footer">
                    <button type="button" className="sc-btn sc-btn--ghost" onClick={onClose}>{t.close}</button>
                </div>
            </div>
        </div>
    );
}
