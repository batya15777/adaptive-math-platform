import { useState } from 'react';
import { format } from '../../i18n/languages.js';
import './HomeHero.css';

// Optional custom rocket artwork: drop a file at client/public/rocket-hero.png
// (ideally a transparent PNG) and it replaces the built-in SVG automatically.
const ROCKET_IMG = '/rocket-hero.png';

// Faint starfield positions (left%, top%) + which ones render a touch larger.
const HERO_STARS = [
    ['8%', '24%'], ['14%', '58%'], ['6%', '78%'], ['21%', '34%'], ['28%', '70%'], ['35%', '16%', 'lg'],
    ['46%', '50%'], ['53%', '24%'], ['58%', '66%', 'lg'], ['64%', '13%'], ['70%', '42%'], ['76%', '72%'],
    ['83%', '20%', 'lg'], ['88%', '54%'], ['94%', '32%'], ['49%', '82%'], ['18%', '12%'], ['90%', '76%'],
];

// Decorative rocket — a detailed SVG (shaded body, window, fins, flame trail) so it
// reads as a polished illustration rather than a flat icon or emoji. Used as the
// fallback when no custom rocket-hero.png is present.
const RocketSvg = () => (
    <svg className="home-hero-rocket" viewBox="0 0 64 90" fill="none" aria-hidden="true">
        <defs>
            <linearGradient id="hh-body" x1="20" y1="0" x2="44" y2="0" gradientUnits="userSpaceOnUse">
                <stop offset="0" stopColor="#c6b4ee" />
                <stop offset=".48" stopColor="#ffffff" />
                <stop offset="1" stopColor="#b6a2e4" />
            </linearGradient>
            <linearGradient id="hh-nose" x1="32" y1="4" x2="32" y2="22" gradientUnits="userSpaceOnUse">
                <stop offset="0" stopColor="#ffb27a" />
                <stop offset="1" stopColor="#ff7aa2" />
            </linearGradient>
            <radialGradient id="hh-win" cx=".4" cy=".34" r=".75">
                <stop offset="0" stopColor="#d6efff" />
                <stop offset="1" stopColor="#2f78d8" />
            </radialGradient>
            <linearGradient id="hh-flame" x1="32" y1="56" x2="32" y2="90" gradientUnits="userSpaceOnUse">
                <stop offset="0" stopColor="#9bdcff" />
                <stop offset=".5" stopColor="#5b8cff" />
                <stop offset="1" stopColor="#7c4dff" stopOpacity="0" />
            </linearGradient>
        </defs>
        {/* flame trail */}
        <path d="M26 56 Q32 88 38 56 Q32 65 26 56 Z" fill="url(#hh-flame)" />
        {/* fins */}
        <path d="M23 45 L12 60 L25 51 Z" fill="#7C4DFF" />
        <path d="M41 45 L52 60 L39 51 Z" fill="#6d3ff0" />
        {/* body */}
        <path d="M32 4 C44 16 46 38 41 55 L23 55 C18 38 20 16 32 4 Z" fill="url(#hh-body)" />
        {/* nose cone */}
        <path d="M32 4 C37 9 40 15 41 21 L23 21 C24 15 27 9 32 4 Z" fill="url(#hh-nose)" />
        {/* window */}
        <circle cx="32" cy="30" r="6.4" fill="url(#hh-win)" />
        <circle cx="32" cy="30" r="6.4" fill="none" stroke="#ffffff" strokeWidth="2.2" />
        {/* base ring */}
        <rect x="23" y="52" width="18" height="4" rx="2" fill="#a890dc" />
    </svg>
);

// Top "galaxy" intro banner for the student Home. Presentational only — all data,
// strings (homeStrings), gender picker `g` and navigation are passed in by Home.
export function HomeHero({ displayName, continueTopic, t, g, onPlay }) {
    const [rocketImgFailed, setRocketImgFailed] = useState(false);
    return (
        <section className="home-hero">
            <div className="home-hero-art" aria-hidden="true">
                <span className="home-hero-glow" />
                <span className="home-hero-planet" />
                {rocketImgFailed
                    ? <RocketSvg />
                    : <img className="home-hero-rocketimg" src={ROCKET_IMG} alt="" aria-hidden="true" onError={() => setRocketImgFailed(true)} />}
                {HERO_STARS.map((p, i) => <i key={i} className={p[2] || ''} style={{ left: p[0], top: p[1] }} />)}
            </div>

            <span className="home-hero-name">{format(t.wave, { name: displayName })}</span>

            <div className="home-hero-content">
                <div className="home-hero-text">
                    <h1>{g(t.journeyTitle)}</h1>
                    <p>{g(t.heroMotivation)}</p>
                    <button type="button" className="sc-btn" onClick={() => onPlay(continueTopic?.subSubjectId)}>{g(t.journeyBtn)}</button>
                </div>
            </div>
        </section>
    );
}
