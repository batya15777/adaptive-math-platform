// Shared game constants — never scatter these magic numbers across components.
export const GAME_ENTRY_COST = 200;       // stars to enter a battle (deducted server-side)
export const PLAYER_MAX_LIVES = 5;        // wrong answers allowed before the player is out
export const STREAK_FOR_SUPER = 3;        // correct-in-a-row needed to trigger a super attack
export const SUPER_ATTACK_DAMAGE = 2;     // monster HP lost on a super attack
export const NORMAL_ATTACK_DAMAGE = 1;    // monster HP lost on a normal correct answer
export const GALAXY_BATTLE_KEY = 'galaxy-battle';
export const ANSWER_GRACE_SECONDS = 20;    // calm time before the monster starts charging
export const MONSTER_WARNING_SECONDS = 10; // countdown while the monster charges an attack

// ── Galaxy Battle topic support (UI gate, GALAXY BATTLE ONLY) ───────────────────────────────
// The battle uses the server's rule-based generator, which produces CORRECT questions only for
// arithmetic (CalculationGenerator: add/sub/mult/div/mixed) and polynomial equations
// (PolynomialGenerator: linear/quadratic). Other sub-subjects (e.g. "Percentages") silently fall
// back to addition, so the battle hides them until the generator supports them. Scoped to Galaxy
// Battle ONLY — Percentages stays everywhere else (admin, regular practice, etc.). The specific
// name keeps this from being reused as a generic "supported topics" list.
export const GALAXY_BATTLE_SUPPORTED_TOPIC_KEYS = ['add', 'sub', 'mult', 'div', 'mixed', 'linear', 'quadratic'];

// Display names whose words don't literally include a key but ARE the same supported topic.
// Kept tiny + explicit so matching stays tight (no broad substring matches).
const GALAXY_BATTLE_TOPIC_ALIASES = { addition: 'add', subtraction: 'sub', multiplication: 'mult', division: 'div' };

// True if a topic's sub-subject name is one the battle generator actually supports. Matches on
// WHOLE words (so "Linear Equations" → ["linear","equations"] → supported via "linear"), never on
// loose substrings (so "Percentages" stays out, and e.g. "subset" would not match "sub").
export const isGalaxyBattleTopicSupported = (name = '') => {
    const words = String(name).toLowerCase().split(/[^a-z]+/).filter(Boolean);
    return words.some(w => GALAXY_BATTLE_SUPPORTED_TOPIC_KEYS.includes(w)
        || GALAXY_BATTLE_TOPIC_ALIASES[w] !== undefined);
};
