// Player ship — rendered from the real PNG asset (transparent bg). The animation classes
// (idle float lives on the wrapper; firing / hit / charging / down here) are unchanged, so all
// existing battle FX keep working — only the artwork changed.
export const PlayerShip = ({ firing = false, hit = false, charging = false, down = false }) => (
    <img
        className={'gb-ship' + (firing ? ' is-firing' : '') + (hit ? ' is-hit' : '') + (charging ? ' is-charging' : '') + (down ? ' is-down' : '')}
        src="/assets/games/galaxy-battle/ship-fighter.png"
        alt=""
        aria-hidden="true"
        draggable="false"
    />
);

export default PlayerShip;
