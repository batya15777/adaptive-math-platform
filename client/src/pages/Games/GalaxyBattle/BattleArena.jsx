import { format } from '../../../i18n/languages.js';
import { MONSTER_WARNING_SECONDS } from '../../../utils/gameConstants.js';
import { BattleHud } from './BattleHud.jsx';
import { PlayerShip } from './PlayerShip.jsx';
import { MonsterSprite } from './MonsterSprite.jsx';
import { BattleQuestionPanel } from './BattleQuestionPanel.jsx';

// Composes the battle screen from small pieces. Presentational only: every value comes from
// `bs` (battleEngine) and answers go up through onAnswer (logic stays in the parent).
export const BattleArena = ({ question, topicName, bs, feedback, locked, countdown, monsterCharge, onAnswer, stars, t }) => {
    const PLAYER_MAX = 5;
    const hearts = Array.from({ length: PLAYER_MAX }, (_, i) => i < bs.playerLives);
    const monsterPct = Math.max(0, Math.round((bs.monsterHp / bs.monsterMaxHp) * 100));
    const hitMonster = feedback?.type === 'correct';
    const hitPlayer = feedback?.type === 'wrong';
    const isSuper = Boolean(feedback?.super);
    const damage = bs.superUsed ? 2 : 1;
    const monsterState = monsterPct <= 25 ? 'angry' : monsterPct <= 55 ? 'damaged' : 'normal';
    const counting = countdown != null;
    const charging = monsterCharge != null;   // monster is charging an attack (time pressure)
    const defeated = bs.monsterHp <= 0;   // monster down → explode
    const shipDown = bs.playerLives <= 0; // player out → ship dims

    const arenaClass = 'gb-arena'
        + (hitMonster ? ' is-hit-monster' : '')
        + (hitPlayer ? ' is-hit-player' : '')
        + (isSuper ? ' is-super' : '')
        + (charging ? ' is-monster-charging' : '');

    return (
        <div className={arenaClass}>
            <div className="gb-spacefx" aria-hidden="true" />

            <BattleHud stars={stars} topicName={topicName} bs={bs} t={t} />

            {/* compact floating timer badge (kept compact, NOT full-width) with a small countdown bar */}
            {charging && (
                <div className={'gb-warn' + (monsterCharge <= 5 ? ' is-urgent' : '')}>
                    <span className="gb-warn-ic" aria-hidden="true">⏳</span>
                    <span className="gb-warn-label">{t.timeRunningOut}</span>
                    <span className="gb-warn-num">{`00:${String(Math.max(0, monsterCharge)).padStart(2, '0')}`}</span>
                    <span className="gb-warn-bar" aria-hidden="true">
                        {Array.from({ length: MONSTER_WARNING_SECONDS }).map((_, i) => (
                            <i key={i} className={i < monsterCharge ? 'is-on' : ''} />
                        ))}
                    </span>
                </div>
            )}

            {/* arena */}
            <div className="gb-fight">
                <div className="gb-fighter gb-fighter--player">
                    <div className="gb-fbar">
                        <span className="gb-fname">{t.shipLabel}</span>
                        <span className="gb-hearts" aria-label={t.livesLabel}>
                            {hearts.map((alive, i) => (
                                <span key={i} className={'gb-heart' + (alive ? '' : ' is-lost')} aria-hidden="true">{alive ? '❤️' : '🤍'}</span>
                            ))}
                        </span>
                    </div>
                    <div className={'gb-char gb-char--player' + (shipDown ? ' is-down' : '')}>
                        <PlayerShip firing={hitMonster} hit={hitPlayer} charging={bs.superReady} down={shipDown} />
                    </div>
                    {bs.streak >= 2 && <div className="gb-combo-badge">{t.combo} <b>x{bs.streak}</b></div>}
                </div>

                <div className="gb-beam" aria-hidden="true">
                    <i className="gb-beam-core" />
                    <i className="gb-spark gb-spark--1" /><i className="gb-spark gb-spark--2" /><i className="gb-spark gb-spark--3" />
                </div>

                <div className="gb-fighter gb-fighter--monster">
                    <div className="gb-fbar">
                        <span className="gb-fname">💀 {format(t.monsterOf, { topic: topicName || '' })}</span>
                        <span className="gb-hpbar"><i style={{ width: `${monsterPct}%` }} /><b>{bs.monsterHp}/{bs.monsterMaxHp}</b></span>
                    </div>
                    <div className={'gb-char gb-char--monster' + (defeated ? ' is-defeated' : '')}>
                        <MonsterSprite state={monsterState} hit={hitMonster} attacking={hitPlayer} defeated={defeated} />
                        {hitMonster && !defeated && <span key={bs.monsterHp} className={'gb-dmg' + (isSuper ? ' is-super' : '')}>-{damage}</span>}
                        {defeated && <span className="gb-boom" aria-hidden="true">💥</span>}
                    </div>
                </div>
            </div>

            {/* question command panel */}
            <BattleQuestionPanel question={question} feedback={feedback} locked={locked || counting} onAnswer={onAnswer} t={t} />

            {/* hit toast */}
            {hitPlayer && <div className="gb-toast" key={bs.playerLives}>{t.hurtToast}</div>}

            {/* 3·2·1·FIGHT countdown */}
            {counting && (
                <div className="gb-countdown" aria-hidden="true">
                    <span key={String(countdown)} className={'gb-countdown-num' + (countdown === 'go' ? ' is-go' : '')}>
                        {countdown === 'go' ? t.fight : countdown}
                    </span>
                </div>
            )}
        </div>
    );
};

export default BattleArena;
