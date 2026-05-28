import { createContext, useState, useContext } from 'react';

const ProfileContext = createContext();

export const ProfileProvider = ({ children }) => {
    // Initial state simulating fetched user data
    const [profileData, setProfileData] = useState({
        name: 'Roman Kovalchuk',
        email: 'roman@example.com',
        preferences: {
            language: 'he', // 'he', 'en', or 'ru'
            solutionDetailLevel: 'detailed', // 'basic', 'moderate', or 'detailed'
        }
    });

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

// Custom hook for easier access
export const useProfile = () => useContext(ProfileContext);