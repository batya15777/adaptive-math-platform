import './avatarBubble.css';

// Renders one avatar at `size` px. Catalog avatars are professional DiceBear illustrations
// (entry.image is a stable data URI). Shared by the profile card + the store.
export function AvatarBubble({ entry, size = 56, alt = '', className = '' }) {
    const src = entry?.image || entry?.img;
    if (src) {
        return <img src={src} alt={alt} className={`av-bubble ${className}`} style={{ width: size, height: size }} draggable={false} />;
    }
    // Defensive fallback only (every catalog avatar has an image).
    return <span className={`av-bubble av-ph ${className}`} style={{ width: size, height: size }} role="img" aria-label={alt}>★</span>;
}
