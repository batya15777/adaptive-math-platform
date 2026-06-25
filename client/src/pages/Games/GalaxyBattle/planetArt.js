// Planet art for the battle topic cards — kept out of the component files (lint: components-only
// exports) so each topic gets a DISTINCT, stable planet: no two topics share the same art, and a
// newly added topic always gets its own planet rather than repeating another topic's.

// Known topics get a stable, meaningful planet (e.g. Linear Equations → Earth). Distinct emojis.
const NAMED_PLANETS = [
    { test: /\blinear\b/i, emoji: '🌍' },              // Linear Equations → Earth (must stay Earth)
    { test: /\bquadratic\b/i, emoji: '🪐' },           // Saturn
    { test: /\badd\b|addition/i, emoji: '🌕' },        // full moon
    { test: /\bsub\b|subtraction/i, emoji: '🌑' },     // dark moon
    { test: /\bmult\b|multiplication/i, emoji: '☄️' }, // comet
    { test: /\bdiv\b|division/i, emoji: '⭐' },        // star
    { test: /\bmixed\b/i, emoji: '🌙' },               // crescent
    { test: /percent/i, emoji: '🔴' },                 // red (hidden from battle, mapped for safety)
];

// Distinct planets for any other (e.g. newly added) topic — none overlap the named set above.
const PLANET_POOL = ['🟣', '🌟', '🌎', '🌏', '💫', '🔵', '🟠', '🟢', '⚪'];

export const PLANET_FALLBACK = '🪐';

// Assigns a planet to every topic so NO TWO topics in the list share the same art — including
// brand-new topics. Known topics keep their meaningful planet; the rest take a unique pool planet.
export const assignPlanets = (topics = []) => {
    const used = new Set();
    const planets = topics.map((topic) => {
        const hit = NAMED_PLANETS.find((p) => p.test.test(String(topic?.name)));
        if (hit) { used.add(hit.emoji); return hit.emoji; }
        return null;
    });
    let next = 0;
    return planets.map((emoji) => {
        if (emoji) return emoji;
        while (next < PLANET_POOL.length && used.has(PLANET_POOL[next])) next++;
        const pick = next < PLANET_POOL.length ? PLANET_POOL[next] : PLANET_POOL[used.size % PLANET_POOL.length];
        used.add(pick); next++;
        return pick;
    });
};
