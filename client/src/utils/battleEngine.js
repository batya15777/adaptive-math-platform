import {
    PLAYER_MAX_LIVES,
    STREAK_FOR_SUPER,
    SUPER_ATTACK_DAMAGE,
    NORMAL_ATTACK_DAMAGE,
} from './gameConstants.js';

// Pure battle logic — no React, no I/O — so it's easy to test and reason about.
// State is immutable: every function returns a NEW state object.

export const createBattleState = (monsterMaxHp) => ({
    playerLives: PLAYER_MAX_LIVES,
    monsterMaxHp,
    monsterHp: monsterMaxHp,
    streak: 0,            // current correct-in-a-row
    superReady: false,    // the NEXT correct answer will be a super attack
    lastHit: null,        // 'monster' | 'player' — who was just hit (drives the attack animation)
    superUsed: false,     // the answer just applied was a super attack
    outcome: null,        // 'win' | 'lose' | null
});

// Apply one answer. Correct → monster takes damage (double on a super attack); a 3rd correct in
// a row IS the super attack and then resets the streak. Wrong → player loses a life, streak resets.
export const applyAnswer = (state, isCorrect) => {
    if (state.outcome) return state; // battle already over — ignore late answers

    let { playerLives, monsterHp, monsterMaxHp, streak } = state;
    let lastHit;
    let superUsed = false;

    if (isCorrect) {
        const isSuper = streak + 1 >= STREAK_FOR_SUPER;
        const damage = isSuper ? SUPER_ATTACK_DAMAGE : NORMAL_ATTACK_DAMAGE;
        monsterHp = Math.max(0, monsterHp - damage);
        streak = isSuper ? 0 : streak + 1; // a super attack consumes the streak
        superUsed = isSuper;
        lastHit = 'monster';
    } else {
        playerLives = Math.max(0, playerLives - 1);
        streak = 0;
        lastHit = 'player';
    }

    const outcome = monsterHp <= 0 ? 'win' : playerLives <= 0 ? 'lose' : null;
    const superReady = !outcome && streak === STREAK_FOR_SUPER - 1;

    return { playerLives, monsterMaxHp, monsterHp, streak, superReady, superUsed, lastHit, outcome };
};

// Difficulty tier for a planet card, derived from the student's level in that topic.
export const monsterTier = (level) => (level <= 2 ? 'easy' : level <= 4 ? 'medium' : 'hard');
