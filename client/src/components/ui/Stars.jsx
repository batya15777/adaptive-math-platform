import { useMemo } from "react";
import { createStarfield } from "../../utils/starfield.js";

// Decorative deterministic starfield for the galaxy background. Render inside a
// ".mg-space" themed root (it positions absolutely as ".sc-bg").
export function Stars({ count = 54, seed = 7 }) {
    const stars = useMemo(() => createStarfield(count, seed), [count, seed]);

    return (
        <div className="sc-bg" aria-hidden="true">
            {stars.map((st, i) => (
                <i key={i} className={st.lg ? "lg" : ""} style={{ left: st.left, top: st.top, opacity: st.o }} />
            ))}
        </div>
    );
}
