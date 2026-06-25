import { format } from '../../../i18n/languages.js';
import { PlanetCard } from './PlanetCard.jsx';
import { assignPlanets } from './planetArt.js';

// "Choose a planet to battle" — a cinematic world-select, not a plain grid. The recommended
// planet (weakest / suggested topic) is highlighted. Generic over the student's topics.
export const TopicPlanetSelector = ({ topics, recommendedId, onSelect, loading, t, g }) => {
    // Distinct planet art per topic (no two share, including newly added topics).
    const planets = assignPlanets(topics);
    return (
    <div className="tps">
        <header className="tps-head">
            <h1 className="tps-title">{t.chooseTopic}</h1>
            <p className="tps-sub">{t.chooseTopicSub}</p>
            <div className="tps-steps">
                <span className="tps-steps-label">{format(t.stepOf, { n: 2, total: 2 })}</span>
            </div>
        </header>

        {topics.length === 0 ? (
            <p className="gb-msg">{t.noBattleTopics}</p>
        ) : (
            <div className="tps-grid">
                {topics.map((tp, i) => (
                    <PlanetCard
                        key={tp.subSubjectId}
                        topic={tp}
                        planet={planets[i]}
                        recommended={tp.subSubjectId === recommendedId}
                        onSelect={onSelect}
                        disabled={loading}
                        t={t}
                        g={g}
                    />
                ))}
            </div>
        )}

        <p className="tps-helper">{t.recommendedHelper}</p>
    </div>
    );
};

export default TopicPlanetSelector;
