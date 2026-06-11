import { useState, useContext, useEffect } from 'react';
import { ProfileContext } from './ProfileContextSetup.js';
import { AuthContext } from '../context/AuthContext.jsx';

export const ProfileProvider = ({ children }) => {
    const { user } = useContext(AuthContext);

    // Initial state using authenticated user data
    const [profileData, setProfileData] = useState({
        name: '',
        email: '',
        preferences: {
            language: 'he',
            solutionDetailLevel: 'detailed',
        }
    });

    // Update profile data when user logs in
    useEffect(() => {
        if (user) {
            setProfileData({
                ...profileData,
                name: user.fullName || user.name || '',
                email: user.email || '',
            });
        }
    }, [user?.email, user?.fullName, user?.name]);

    // Function to update the profile data
    const updateProfile = (newData) => {
        setProfileData((prev) => ({ ...prev, ...newData }));
    };

    return (
        <ProfileContext.Provider value={{ profileData, updateProfile }}>
            {children}
        </ProfileContext.Provider>
    );
};

