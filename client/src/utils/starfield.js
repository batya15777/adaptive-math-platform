// Pure deterministic decoration data shared by every galaxy background.
export function createStarfield(count, seed = 7) {
    let current = seed;
    const random = () => {
        current = (current * 9301 + 49297) % 233280;
        return current / 233280;
    };

    return Array.from({ length: count }, () => ({
        left: `${(random() * 100).toFixed(2)}%`,
        top: `${(random() * 100).toFixed(2)}%`,
        lg: random() < 0.12,
        o: (0.3 + random() * 0.6).toFixed(2),
    }));
}
