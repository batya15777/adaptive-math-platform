import levelUpSound from '../assets/sound/level-up.mp3';

// Plays the celebratory "level up / success" chime. A fresh Audio object is created on
// every call — a stored ref can be blocked by the browser's autoplay policy after an
// async gap, so re-creating it each time keeps playback reliable. Errors (e.g. autoplay
// still blocked before any user gesture) are swallowed so they never break the flow.
// Shared by the regular practice (level-up) and the daily practice (5-correct finish).
export const playLevelUpSound = () => {
    const audio = new Audio(levelUpSound);
    audio.play().catch(() => {});
};
