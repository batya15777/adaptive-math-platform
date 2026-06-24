import api from './api.js';

// Start a Galaxy Battle: server deducts GAME_ENTRY_COST stars (402 if not enough) and returns
// the battle setup (updated stars, level, monster HP, lives, and the question batch).
export const startGalaxyBattle = (subSubjectId, language) =>
    api.post('/games/galaxy-battle/start', { subSubjectId, language });

// Record the outcome (history + badge). Never changes level progression or grants stars.
export const finishGalaxyBattle = (subSubjectId, level, won) =>
    api.post('/games/galaxy-battle/finish', { subSubjectId, level, won });
