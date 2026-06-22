import { useProfile } from "../../contexts/useProfile.js";

// Reusable language selector. Persists the choice to the user's profile — the single
// source of truth — so the entire app follows it, not just where this is rendered.
// Options come from the backend (/profile/options), so new languages need no UI change.
export const LanguageSwitcher = () => {
    const { profileData, options, updateProfile, loading } = useProfile();

    const onChange = (e) => {
        const language = e.target.value;
        if (language !== profileData.language) {
            // Errors are surfaced through the profile context's own error state.
            updateProfile({ language }).catch(() => {});
        }
    };

    // Fall back to the current value if options haven't loaded yet.
    const languages = options.languages?.length
        ? options.languages
        : [{ value: profileData.language, label: profileData.language }];

    return (
        <label style={{ display: "inline-flex", alignItems: "center", gap: 8, fontSize: 14 }}>
            <span aria-hidden="true">🌐</span>
            <select
                value={profileData.language}
                onChange={onChange}
                disabled={loading}
                aria-label="Language"
                style={{ padding: "4px 8px", borderRadius: 6 }}
            >
                {languages.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                ))}
            </select>
        </label>
    );
};
