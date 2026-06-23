import { useState, useContext, useEffect, useCallback } from 'react';
import { ProfileContext } from './ProfileContextSetup.js';
import { AuthContext } from '../context/AuthContextSetup.js';
import { getProfile, updateProfile as updateProfileApi, getProfileOptions, selectAvatar as selectAvatarApi } from '../service/profileApi.js';
import { readGuestLanguage, writeGuestLanguage, readGuestTheme, writeGuestTheme } from '../i18n/languages.js';

const DEFAULT_PROFILE = {
    theme:     'LIGHT',
    language:  'HEBREW',
    pictureId: 0,
    totalStars: 0,
    selectedAvatarId: null,
    ownedAvatarIds: [],
};

const DEFAULT_OPTIONS = { themes: [], languages: [] };

// Normalize a /profile (or /profile/avatar/select) response into our profile state shape.
const fromRes = (d) => ({
    theme:            d.theme,
    language:         d.language,
    pictureId:        d.pictureId,
    totalStars:       d.totalStars ?? 0,
    selectedAvatarId: d.selectedAvatarId ?? null,
    ownedAvatarIds:   d.ownedAvatarIds ?? [],
});

export const ProfileProvider = ({ children }) => {
    const { user, loginUser } = useContext(AuthContext);

    const [profile, setProfile]   = useState(() => ({ ...DEFAULT_PROFILE, theme: readGuestTheme(), language: readGuestLanguage() }));
    const [options, setOptions]   = useState(DEFAULT_OPTIONS);
    const [loading, setLoading]   = useState(false);
    const [error,   setError]     = useState(null);

    // Theme/language labels are static metadata — fetch once on mount
    useEffect(() => {
        getProfileOptions()
            .then(res => setOptions(res.data))
            .catch(() => { /* keep defaults; selects fall back to raw values */ });
    }, []);

    // Fetch from server whenever the user logs in
    useEffect(() => {
        if (!user) {
            // Guest: theme + language come from localStorage so they survive a refresh.
            // This intentionally synchronizes profile state with an auth-state transition.
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setProfile({ ...DEFAULT_PROFILE, theme: readGuestTheme(), language: readGuestLanguage() });
            return;
        }
        setLoading(true);
        getProfile()
            .then(res => {
                setProfile(fromRes(res.data));
                setError(null);
            })
            .catch(() => setError('Failed to load profile.'))
            .finally(() => setLoading(false));
    }, [user]);

    // Sends only the changed fields to the server, then updates local state
    const updateProfile = useCallback(async (changes) => {
        // Guest (no session): persist theme/language locally instead of calling the API.
        if (!user) {
            if (changes.language) writeGuestLanguage(changes.language);
            if (changes.theme) writeGuestTheme(changes.theme);
            setProfile(prev => ({ ...prev, ...changes }));
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const res = await updateProfileApi(changes);
            setProfile(fromRes(res.data));
            // Name + gender live on the auth user (the server returns fullName as `username`).
            // Sync it so the UI updates immediately; a refresh re-fetches the persisted values.
            const newName = changes.fullName != null && changes.fullName.trim() !== '' ? changes.fullName.trim() : null;
            const newGender = changes.gender != null ? changes.gender : null;
            if (user && (newName != null || newGender != null)) {
                loginUser({ ...user, username: newName ?? user.username, gender: newGender ?? user.gender });
            }
        } catch {
            setError('Failed to save profile.');
            throw new Error('Failed to save profile.');
        } finally {
            setLoading(false);
        }
    }, [user, loginUser]);

    // Select (and buy, if needed) an avatar. The server deducts stars + records ownership;
    // we just reflect the returned state. Throws on failure (e.g. not enough stars).
    const selectAvatar = useCallback(async (avatarId) => {
        if (!user) return null;
        const res = await selectAvatarApi(avatarId);
        setProfile(fromRes(res.data));
        return res.data; // includes the updated totalStars (so callers can refresh the display)
    }, [user]);

    // Expose name/email from auth alongside the server-backed settings
    const profileData = {
        name:             user?.fullName || user?.username || user?.name || '',
        email:            user?.email || '',
        theme:            profile.theme,
        language:         profile.language,
        pictureId:        profile.pictureId,
        stars:            profile.totalStars ?? 0,
        selectedAvatarId: profile.selectedAvatarId ?? null,
        ownedAvatarIds:   profile.ownedAvatarIds ?? [],
    };

    return (
        <ProfileContext.Provider value={{ profileData, options, updateProfile, selectAvatar, loading, error }}>
            {children}
        </ProfileContext.Provider>
    );
};
