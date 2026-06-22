import { useState, useContext, useEffect, useCallback } from 'react';
import { ProfileContext } from './ProfileContextSetup.js';
import { AuthContext } from '../context/AuthContextSetup.js';
import { getProfile, updateProfile as updateProfileApi, getProfileOptions } from '../service/profileApi.js';
import { readGuestLanguage, writeGuestLanguage, readGuestTheme, writeGuestTheme } from '../i18n/languages.js';

const DEFAULT_PROFILE = {
    theme:     'LIGHT',
    language:  'HEBREW',
    pictureId: 0,
};

const DEFAULT_OPTIONS = { themes: [], languages: [] };

export const ProfileProvider = ({ children }) => {
    const { user } = useContext(AuthContext);

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
            setProfile({ ...DEFAULT_PROFILE, theme: readGuestTheme(), language: readGuestLanguage() });
            return;
        }
        setLoading(true);
        getProfile()
            .then(res => {
                const { theme, language, pictureId } = res.data;
                setProfile({ theme, language, pictureId });
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
            const { theme, language, pictureId } = res.data;
            setProfile({ theme, language, pictureId });
        } catch {
            setError('Failed to save profile.');
            throw new Error('Failed to save profile.');
        } finally {
            setLoading(false);
        }
    }, [user]);

    // Expose name/email from auth alongside the server-backed settings
    const profileData = {
        name:      user?.fullName || user?.name || '',
        email:     user?.email || '',
        theme:     profile.theme,
        language:  profile.language,
        pictureId: profile.pictureId,
    };

    return (
        <ProfileContext.Provider value={{ profileData, options, updateProfile, loading, error }}>
            {children}
        </ProfileContext.Provider>
    );
};
