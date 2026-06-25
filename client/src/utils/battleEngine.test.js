// Unit tests for the pure Galaxy Battle logic. Run: `node --test src/utils`
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createBattleState, applyAnswer, monsterTier } from './battleEngine.js';

test('createBattleState: full lives, full monster HP, no streak', () => {
    const s = createBattleState(8);
    assert.equal(s.playerLives, 5);
    assert.equal(s.monsterHp, 8);
    assert.equal(s.monsterMaxHp, 8);
    assert.equal(s.streak, 0);
    assert.equal(s.outcome, null);
});

test('correct answer damages the monster, not the player', () => {
    const s = applyAnswer(createBattleState(8), true);
    assert.equal(s.monsterHp, 7);
    assert.equal(s.playerLives, 5);
    assert.equal(s.lastHit, 'monster');
    assert.equal(s.streak, 1);
});

test('wrong answer costs a life and resets the streak', () => {
    let s = createBattleState(8);
    s = applyAnswer(s, true);   // streak 1
    s = applyAnswer(s, false);  // wrong
    assert.equal(s.playerLives, 4);
    assert.equal(s.streak, 0);
    assert.equal(s.lastHit, 'player');
});

test('3 correct in a row → super attack (double damage), streak resets', () => {
    let s = createBattleState(10);
    s = applyAnswer(s, true);  // hp 9, streak 1
    s = applyAnswer(s, true);  // hp 8, streak 2 → superReady
    assert.equal(s.superReady, true);
    s = applyAnswer(s, true);  // SUPER: hp 8-2=6, streak resets
    assert.equal(s.monsterHp, 6);
    assert.equal(s.superUsed, true);
    assert.equal(s.streak, 0);
});

test('win when the monster reaches 0 HP', () => {
    let s = createBattleState(2);
    s = applyAnswer(s, true); // hp 1
    s = applyAnswer(s, true); // hp 0 → win
    assert.equal(s.monsterHp, 0);
    assert.equal(s.outcome, 'win');
});

test('lose after 5 wrong answers', () => {
    let s = createBattleState(8);
    for (let i = 0; i < 5; i++) s = applyAnswer(s, false);
    assert.equal(s.playerLives, 0);
    assert.equal(s.outcome, 'lose');
});

test('answers after the battle is over are ignored', () => {
    let s = createBattleState(1);
    s = applyAnswer(s, true);        // win
    const after = applyAnswer(s, false);
    assert.equal(after.outcome, 'win');
    assert.equal(after.playerLives, 5);
});

test('monsterTier scales with level', () => {
    assert.equal(monsterTier(1), 'easy');
    assert.equal(monsterTier(4), 'medium');
    assert.equal(monsterTier(7), 'hard');
});
