import { useMemo } from "react";

// Decorative deterministic starfield for the galaxy background. Render inside a
// ".mg-space" themed root (it positions absolutely as ".sc-bg").
export function Stars({ count = 54, seed = 7 }) {
    const stars = useMemo(() => {
        let s = seed;
        const rnd = () => (s = (s * 9301 + 49297) % 233280) / 233280;
        return Array.from({ length: count }, () => ({
            left: (rnd() * 100).toFixed(2) + "%",
            top: (rnd() * 100).toFixed(2) + "%",
            lg: rnd() < 0.12,
            o: (0.3 + rnd() * 0.6).toFixed(2),
        }));
    }, [count, seed]);

    return (
        <div className="sc-bg" aria-hidden="true">
            {stars.map((st, i) => (
                <i key={i} className={st.lg ? "lg" : ""} style={{ left: st.left, top: st.top, opacity: st.o }} />
            ))}
        </div>
    );
}
