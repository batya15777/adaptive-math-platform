import { RocketSvg } from '../ui/RocketSvg.jsx';
import { format } from '../../i18n/languages.js';
import './LevelRocketProgress.css';

// Rocket-themed level progress: a horizontal journey from the current level (a planet) to
// the next level (a galaxy). The rocket sits at `current / target` along the track and
// glides forward on every correct answer (the parent only advances `current` when the
// server confirms a correct answer, so the rocket never moves on a wrong one).
//
// The path is physically left→right in every language (dir="ltr") — a rocket journey reads
// the same way regardless of the page's text direction; only the labels are translated.
//   currentLevel  the level the student is on            current  correct answers gathered
//   target        correct answers needed for next level  inSubLevel  bar paused (easier practice)
//   celebrate     show the "reached next level" cheer     t        questionGameStrings dict
export function LevelRocketProgress({ currentLevel, current, target, inSubLevel = false, celebrate = false, t }) {
    const safeTarget = Math.max(1, target || 0);
    const done = Math.max(0, Math.min(current || 0, safeTarget));
    const pct = Math.round((done / safeTarget) * 100);
    const reached = done >= safeTarget;

    // One milestone dot per required answer (capped so a large target stays tidy).
    const dotCount = Math.min(safeTarget, 30);
    const dots = Array.from({ length: dotCount }, (_, i) => Math.round(((i + 1) / dotCount) * safeTarget) <= done);

    return (
        <div className={`lrp${inSubLevel ? ' lrp--sub' : ''}`} dir="ltr">
            <div className="lrp-caption">
                {format(t.toNextLevel, { current: done, target: safeTarget })}
            </div>

            <div className="lrp-row">
                <div className="lrp-endpoint">
                    <span className="lrp-planet" aria-hidden="true" />
                    <span className="lrp-endpoint-label">{format(t.level, { level: currentLevel })}</span>
                </div>

                <div className="lrp-track">
                    <div className="lrp-rail" aria-hidden="true" />
                    <div className="lrp-rail-fill" style={{ width: `${pct}%` }} aria-hidden="true" />
                    <div className="lrp-dots" aria-hidden="true">
                        {dots.map((filled, i) => (
                            <span key={i} className={`lrp-dot${filled ? ' is-filled' : ''}`} />
                        ))}
                    </div>
                    <div className={`lrp-rocket${reached ? ' is-reached' : ''}`} style={{ left: `${pct}%` }}>
                        <span className="lrp-rocket-glow" aria-hidden="true" />
                        <RocketSvg className="lrp-rocket-svg" />
                    </div>
                </div>

                <div className="lrp-endpoint">
                    <span className={`lrp-galaxy${reached ? ' is-reached' : ''}`} aria-hidden="true" />
                    <span className="lrp-endpoint-label">{format(t.level, { level: currentLevel + 1 })}</span>
                </div>
            </div>

            {inSubLevel && <p className="lrp-note">{t.subLevelPaused}</p>}
            {celebrate && <p className="lrp-cheer">🎉 {t.levelUpCelebrate}</p>}
        </div>
    );
}

export default LevelRocketProgress;
