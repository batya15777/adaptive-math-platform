import { format } from '../../../i18n/languages.js';
import { STREAK_FOR_SUPER } from '../../../utils/gameConstants.js';

// Top game HUD: stars, topic, streak, and the super-attack meter (fills with the streak, %).
export const BattleHud = ({ stars, topicName, bs, t }) => {
    const superPct = Math.round((Math.min(bs.streak, STREAK_FOR_SUPER) / STREAK_FOR_SUPER) * 100);
    return (
        <div className="gb-hud">
            <div className="gb-hud-cell"><span aria-hidden="true">⭐</span> <b>{stars != null ? stars.toLocaleString() : '—'}</b></div>
            <div className="gb-hud-cell gb-hud-cell--topic"><span aria-hidden="true">🪐</span> {topicName}</div>
            <div className="gb-hud-cell"><span aria-hidden="true">🔥</span> {format(t.streak, { count: bs.streak })}</div>
            <div className={'gb-hud-cell gb-hud-cell--super' + (bs.superReady ? ' is-ready' : '')}>
                <span aria-hidden="true">⚡</span>
                <span className="gb-hud-superbar"><i style={{ width: `${superPct}%` }} /></span>
                <b className="gb-hud-superpct">{superPct}%</b>
            </div>
        </div>
    );
};

export default BattleHud;
