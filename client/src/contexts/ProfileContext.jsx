import { useState, useContext, useEffect, useCallback } from 'react';
import { ProfileContext } from './ProfileContextSetup.js';
import { AuthContext } from '../context/AuthContextSetup.js';
import { getProfile, updateProfile as updateProfileApi } from '../service/profileApi.js';

const DEFAULT_PROFILE = {
    theme:     'LIGHT',
    language:  'HEBREW',
    pictureId: 0,
};

export const ProfileProvider = ({ children }) => {
    const { user } = useContext(AuthContext);

    const [profile, setProfile]   = useState(DEFAULT_PROFILE);
    const [loading, setLoading]   = useState(false);
    const [error,   setError]     = useState(null);

    // Fetch from server whenever the user logs in
    useEffect(() => {
        if (!user) {
            setProfile(DEFAULT_PROFILE);
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
    }, []);

    // Expose name/email from auth alongside the server-backed settings
    const profileData = {
        name:      user?.fullName || user?.name || '',
        email:     user?.email || '',
        theme:     profile.theme,
        language:  profile.language,
        pictureId: profile.pictureId,
    };

    return (
        <ProfileContext.Provider value={{ profileData, updateProfile, loading, error }}>
            {children}
        </ProfileContext.Provider>
    );
};
