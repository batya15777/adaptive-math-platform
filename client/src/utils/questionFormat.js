// Shared parsing for question payloads coming from the progress API.
// Single source of truth so every screen (leveled practice, daily practice, …)
// renders the same data identically — no per-component drift.

// Multiple-choice options arrive as a single string joined by the server with "|"
// (see LevelManagerService#String.join("|", …)). Pipe-delimited so an individual
// option may itself contain commas (e.g. "X1=5 , X2=10").
//   "5|7|9|11" → ["5","7","9","11"];  "" / null → []
export const parseOptions = (s) => (s ? s.split('|').map(o => o.trim()).filter(Boolean) : []);

// A worked solution arrives as newline-separated steps.
//   "a = 1\nb = 2" → ["a = 1","b = 2"];  "" / null → []
export const parseSolution = (s) => (s ? s.split('\n').map(o => o.trim()).filter(Boolean) : []);
