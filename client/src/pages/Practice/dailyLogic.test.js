// Unit tests for the pure daily-practice logic. Run: `node --test src/pages/Practice`
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
    orderDailyTopics, topicForIndex, isComplete, progressPct, pickFreshQuestion, DAILY_GOAL,
} from './dailyLogic.js';

test('orderDailyTopics: weakest first, ties broken by fewer attempts', () => {
    const topics = [
        { subSubjectId: 1, name: 'add', successRate: 90, attempts: 10 },
        { subSubjectId: 2, name: 'sub', successRate: 40, attempts: 5 },
        { subSubjectId: 3, name: 'mul', successRate: 40, attempts: 2 },
    ];
    assert.deepEqual(orderDailyTopics(topics).map(t => t.subSubjectId), [3, 2, 1]);
});

test('orderDailyTopics: drops invalid entries and handles empty', () => {
    assert.deepEqual(orderDailyTopics(), []);
    assert.deepEqual(orderDailyTopics([null, { name: 'x' }, undefined]), []);
});

test('orderDailyTopics: does not mutate the input', () => {
    const topics = [{ subSubjectId: 1, successRate: 50 }, { subSubjectId: 2, successRate: 10 }];
    const snapshot = JSON.stringify(topics);
    orderDailyTopics(topics);
    assert.equal(JSON.stringify(topics), snapshot);
});

test('topicForIndex: cycles through topics (mixed) and is null-safe', () => {
    const ordered = [{ subSubjectId: 3 }, { subSubjectId: 2 }];
    assert.equal(topicForIndex(ordered, 0).subSubjectId, 3);
    assert.equal(topicForIndex(ordered, 1).subSubjectId, 2);
    assert.equal(topicForIndex(ordered, 2).subSubjectId, 3);
    assert.equal(topicForIndex(ordered, 5).subSubjectId, 2);
    assert.equal(topicForIndex([], 0), null);
    assert.equal(topicForIndex(null, 0), null);
});

test('isComplete: true only at/after the goal', () => {
    assert.equal(isComplete(DAILY_GOAL), true);
    assert.equal(isComplete(DAILY_GOAL + 1), true);
    assert.equal(isComplete(DAILY_GOAL - 1), false);
    assert.equal(isComplete(0), false);
});

test('pickFreshQuestion: skips an already-seen question and returns a fresh one', async () => {
    const ordered = [{ subSubjectId: 1 }, { subSubjectId: 2 }];
    const served = [{ questionId: 10 }, { questionId: 11 }];
    let n = 0;
    const fetch = async () => served[n++];
    const r = await pickFreshQuestion({ orderedTopics: ordered, startIndex: 0, seen: new Set([10]), fetch });
    assert.equal(r.question.questionId, 11);
    assert.equal(r.fresh, true);
});

test('pickFreshQuestion: falls back to last (no infinite loop) when all are seen', async () => {
    const ordered = [{ subSubjectId: 1 }];
    const fetch = async () => ({ questionId: 10 }); // always the same, already seen
    const r = await pickFreshQuestion({ orderedTopics: ordered, startIndex: 0, seen: new Set([10]), fetch, maxTries: 4 });
    assert.equal(r.question.questionId, 10);
    assert.equal(r.fresh, false);
});

test('pickFreshQuestion: returns null when nothing can be fetched / no topics', async () => {
    assert.equal(await pickFreshQuestion({ orderedTopics: [], seen: new Set(), fetch: async () => null }), null);
});

test('progressPct: clamps to 0..100', () => {
    assert.equal(progressPct(0), 0);
    assert.equal(progressPct(1), 20);
    assert.equal(progressPct(5), 100);
    assert.equal(progressPct(9), 100);
    assert.equal(progressPct(2, 0), 0);
});
