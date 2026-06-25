import { format } from '../../../i18n/languages.js';
import { monsterTier } from '../../../utils/battleEngine.js';
import { RecommendedBadge } from './RecommendedBadge.jsx';
import { PLANET_FALLBACK } from './planetArt.js';

// One "planet" (topic) to battle. Big planet art (assigned by the parent so no two topics share
// art) on a floating platform + level + monster strength + a CTA. The recommended planet is
// highlighted. Generic over the student's topics.
export const PlanetCard = ({ topic, planet, recommended, onSelect, disabled, t, g }) => {
    const level = topic.currentLevel || 1;
    const tier = monsterTier(level);
    const tierLabel = tier === 'easy' ? t.monsterEasy : tier === 'medium' ? t.monsterMedium : t.monsterHard;
    const recLabel = (t.recommended || '').replace(/\s*⚠️\s*$/, '');

    return (
        <div className={'pc' + (recommended ? ' pc--rec' : '')}>
            {recommended && <RecommendedBadge label={recLabel} />}
            <div className="pc-planet">
                <span className="pc-platform" aria-hidden="true" />
                <span className="pc-planet-emoji" aria-hidden="true">{planet || PLANET_FALLBACK}</span>
            </div>
            <div className="pc-name">{topic.name}</div>
            <div className="pc-level">{format(t.yourLevel, { level })}</div>
            <div className="pc-monster">👾 {tierLabel}</div>
            <button
                type="button"
                className={'sc-btn pc-btn' + (recommended ? ' pc-btn--rec' : '')}
                disabled={disabled}
                onClick={() => onSelect(topic)}
            >
                {recommended ? t.continueToBattle : g(t.choose)}
            </button>
        </div>
    );
};

export default PlanetCard;
