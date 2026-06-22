import { format } from '../../i18n/languages.js';
import { RocketSvg } from '../../components/ui/RocketSvg.jsx';
import './HomeHero.css';

// Faint starfield positions (left%, top%) + which ones render a touch larger.
const HERO_STARS = [
    ['8%', '24%'], ['14%', '58%'], ['6%', '78%'], ['21%', '34%'], ['28%', '70%'], ['35%', '16%', 'lg'],
    ['46%', '50%'], ['53%', '24%'], ['58%', '66%', 'lg'], ['64%', '13%'], ['70%', '42%'], ['76%', '72%'],
    ['83%', '20%', 'lg'], ['88%', '54%'], ['94%', '32%'], ['49%', '82%'], ['18%', '12%'], ['90%', '76%'],
];

// Decorative rocket — shared illustration, positioned via the home-hero-rocket class.
const Rocket = () => <RocketSvg className="home-hero-rocket" />;

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
