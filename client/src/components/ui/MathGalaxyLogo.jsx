import "./MathGalaxyLogo.css";

// Single source of truth for the MathGalaxy brand lockup (orbital glyph + wordmark).
// Use everywhere instead of duplicating the SVG. size: "sm" | "md" | "lg" | "xl".
export function MathGalaxyLogo({ size = "md", className = "" }) {
    return (
        <span className={`mgl mgl--${size} ${className}`} aria-label="MathGalaxy">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="12" r="3.4" fill="#7C4DFF" />
                <ellipse cx="12" cy="12" rx="10" ry="4.3" stroke="#7aa2ff" strokeWidth="1.2" opacity=".95" />
                <ellipse cx="12" cy="12" rx="10" ry="4.3" stroke="#A78BFA" strokeWidth="1.2" transform="rotate(60 12 12)" opacity=".8" />
                <ellipse cx="12" cy="12" rx="10" ry="4.3" stroke="#A78BFA" strokeWidth="1.2" transform="rotate(120 12 12)" opacity=".6" />
            </svg>
            <span className="mgl-wm"><b>Math</b><span>Galaxy</span></span>
        </span>
    );
}
