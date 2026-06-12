import { useState, useContext, useCallback } from 'react';
import { ProfileContext } from './ProfileContextSetup.js';
import { AuthContext } from '../context/AuthContextSetup.js';

export const ProfileProvider = ({ children }) => {
    const { user } = useContext(AuthContext);

    // Preference overrides managed by the user (persisted across sessions in the future)
    const [preferenceOverrides, setPreferenceOverrides] = useState({
        name: null,
        preferences: {
            language: 'he',
            solutionDetailLevel: 'detailed',
        }
    });

    // Derive profile data at render time — no useEffect + setState needed.
    // User-identity fields come from the auth context; preference fields come from local state.
    const profileData = {
        name: preferenceOverrides.name ?? (user?.fullName || user?.name || ''),
        email: user?.email || '',
        preferences: preferenceOverrides.preferences,
    };

    // Function to update profile data (preferences and display name)
    const updateProfile = useCallback((newData) => {
        setPreferenceOverrides((prev) => {
            const { preferences: newPrefs, ...rest } = newData;
            return {
                ...prev,
                ...rest,
                preferences: newPrefs
                    ? { ...prev.preferences, ...newPrefs }
                    : prev.preferences,
            };
        });
    }, []);

    return (
        <ProfileContext.Provider value={{ profileData, updateProfile }}>
            {children}
        </ProfileContext.Provider>
    );
};
