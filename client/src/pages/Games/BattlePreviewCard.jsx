import { PlayerShip } from './GalaxyBattle/PlayerShip.jsx';
import { MonsterSprite } from './GalaxyBattle/MonsterSprite.jsx';

// A small, non-interactive replica of the real battle screen — it "sells" the game by showing
// exactly how it looks inside: ship vs monster, HP, and a mini question. Reuses the real assets.
export const BattlePreviewCard = ({ t }) => (
    <div className="gp-prev" aria-hidden="true">
        <div className="gp-prev-stage" dir="ltr">
            <span className="gp-prev-floor" aria-hidden="true" />
            <div className="gp-prev-fighter">
                <span className="gp-prev-name">{t.shipLabel}</span>
                <span className="gp-prev-hearts">❤️❤️<span className="is-lost">🤍</span><span className="is-lost">🤍</span><span className="is-lost">🤍</span></span>
                <span className="gp-prev-char gp-prev-char--ship"><PlayerShip /></span>
            </div>
            <span className="gp-prev-beam" />
            <div className="gp-prev-fighter gp-prev-fighter--mon">
                <span className="gp-prev-name">💀 {t.enemyLabel}</span>
                <span className="gp-prev-hp"><i style={{ width: '100%' }} /><b>5/5</b></span>
                <span className="gp-prev-char gp-prev-char--mon"><MonsterSprite /></span>
            </div>
        </div>
        <div className="gp-prev-q">
            <div className="gp-prev-q-title">{t.previewQuestion}</div>
            <div className="gp-prev-q-expr" dir="ltr">8 + 7 = ?</div>
            <div className="gp-prev-q-opts">
                {['13', '14', '15', '16'].map((o, i) => (
                    <span key={o} className={'gp-prev-opt' + (i === 2 ? ' is-correct' : '')}>{o}</span>
                ))}
            </div>
        </div>
    </div>
);

export default BattlePreviewCard;
