// Boss monster — rendered from the real PNG asset (transparent bg). The state/hit/attacking/
// defeated props still toggle the same classes, so the existing reactions (flash, shake, angry
// glow, explode) keep working — only the artwork changed.
export const MonsterSprite = ({ state = 'normal', hit = false, attacking = false, defeated = false }) => (
    <img
        className={`gb-mon gb-mon--${state}` + (hit ? ' is-hit' : '') + (attacking ? ' is-attacking' : '') + (defeated ? ' is-defeated' : '')}
        src="/assets/games/galaxy-battle/monster-boss.png"
        alt=""
        aria-hidden="true"
        draggable="false"
    />
);

export default MonsterSprite;
