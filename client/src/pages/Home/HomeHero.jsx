import { format } from '../../i18n/languages.js';
import './HomeHero.css';

// Faint starfield positions (left%, top%) + which ones render a touch larger.
const HERO_STARS = [
    ['8%', '24%'], ['14%', '58%'], ['6%', '78%'], ['21%', '34%'], ['28%', '70%'], ['35%', '16%', 'lg'],
    ['46%', '50%'], ['53%', '24%'], ['58%', '66%', 'lg'], ['64%', '13%'], ['70%', '42%'], ['76%', '72%'],
    ['83%', '20%', 'lg'], ['88%', '54%'], ['94%', '32%'], ['49%', '82%'], ['18%', '12%'], ['90%', '76%'],
];

// Decorative rocket — a detailed SVG illustration (shaded body, porthole window with
// rim + highlight, swept fins, gold accent, blue-white flame trail).
const Rocket = () => (
    <svg className="home-hero-rocket" viewBox="0 0 80 126" fill="none" aria-hidden="true">
        <defs>
            <linearGradient id="hh-body" x1="26" y1="10" x2="54" y2="78" gradientUnits="userSpaceOnUse">
                <stop offset="0" stopColor="#ffffff" />
                <stop offset=".5" stopColor="#ece6fb" />
                <stop offset="1" stopColor="#cab6f2" />
            </linearGradient>
            <linearGradient id="hh-nose" x1="40" y1="6" x2="40" y2="28" gradientUnits="userSpaceOnUse">
                <stop offset="0" stopColor="#a98bf0" />
                <stop offset="1" stopColor="#7c4dff" />
            </linearGradient>
            <linearGradient id="hh-fin" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stopColor="#9b6bff" />
                <stop offset="1" stopColor="#6a3fd6" />
            </linearGradient>
            <radialGradient id="hh-win" cx=".38" cy=".34" r=".8">
                <stop offset="0" stopColor="#c7f0ff" />
                <stop offset=".55" stopColor="#3aa0e8" />
                <stop offset="1" stopColor="#1e5fb0" />
            </radialGradient>
            <linearGradient id="hh-flame" x1="40" y1="76" x2="40" y2="126" gradientUnits="userSpaceOnUse">
                <stop offset="0" stopColor="#ffffff" />
                <stop offset=".3" stopColor="#8fd9ff" />
                <stop offset=".7" stopColor="#5b8cff" />
                <stop offset="1" stopColor="#7c4dff" stopOpacity="0" />
            </linearGradient>
        </defs>

        {/* flame trail */}
        <path d="M31 80 C33 104 40 126 40 126 C40 126 47 104 49 80 C45 90 35 90 31 80 Z" fill="url(#hh-flame)" />
        <path d="M35 82 C36 98 40 113 40 113 C40 113 44 98 45 82 C43 89 37 89 35 82 Z" fill="#ffffff" opacity=".75" />

        {/* fins */}
        <path d="M29 58 C21 65 16 73 17 82 C23 78 29 73 32 69 Z" fill="url(#hh-fin)" />
        <path d="M51 58 C59 65 64 73 63 82 C57 78 51 73 48 69 Z" fill="#6a3fd6" />

        {/* body */}
        <path d="M40 6 C54 20 56 50 50 76 L30 76 C24 50 26 20 40 6 Z" fill="url(#hh-body)" />
        <path d="M40 6 C54 20 56 50 50 76 L41 76 C41 50 41 20 40 6 Z" fill="#b89fe6" opacity=".38" />

        {/* nose cone */}
        <path d="M40 6 C46 13 49 20 50 27 L30 27 C31 20 34 13 40 6 Z" fill="url(#hh-nose)" />

        {/* gold accent */}
        <path d="M31 51 L49 51" stroke="#ffd47a" strokeWidth="2.4" strokeLinecap="round" />

        {/* window */}
        <circle cx="40" cy="38" r="10" fill="#cdbcf2" />
        <circle cx="40" cy="38" r="8.2" fill="url(#hh-win)" />
        <ellipse cx="36.6" cy="34.6" rx="3" ry="2" fill="#ffffff" opacity=".7" />

        {/* base ring */}
        <rect x="30" y="73" width="20" height="5" rx="2.5" fill="#a890dc" />
    </svg>
);

// Top "galaxy" intro banner for the student Home. Presentational only — all data,
// strings (homeStrings), gender picker `g` and navigation are passed in by Home.
export function HomeHero({ displayName, continueTopic, t, g, onPlay }) {
    return (
        <section className="home-hero">
            <div className="home-hero-art" aria-hidden="true">
                <span className="home-hero-glow" />
                <span className="home-hero-planet" />
                <Rocket />
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
