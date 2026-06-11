import {useState} from "react";
import {useProfile} from "../../contexts/ProfileContext.jsx";

export const ProfileSettings = () => {
    const { profileData, updateProfile } = useProfile();

    // Local state for toggling edit mode and handling temporary form data
    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState(profileData);

    // Handle generic input changes, handling nested preference objects
    const handleChange = (e) => {
        const { name, value } = e.target;

        if (name === 'language' || name === 'solutionDetailLevel') {
            setFormData({
                ...formData,
                preferences: { ...formData.preferences, [name]: value }
            });
        } else {
            setFormData({ ...formData, [name]: value });
        }
    };

    // Commit changes to the global context
    const handleSave = () => {
        updateProfile(formData);
        setIsEditing(false);
    };

    // Discard changes and revert to current context data
    const handleCancel = () => {
        setFormData(profileData);
        setIsEditing(false);
    };

    return (
        <div style={{ padding: '20px', maxWidth: '500px', margin: '0 auto', fontFamily: 'sans-serif' }}>
            <h2>ניהול פרופיל (Profile Management)</h2>

            {!isEditing ? (
                <div style={{ border: '1px solid #ccc', padding: '15px', borderRadius: '8px' }}>
                    <p><strong>Name:</strong> {profileData.name}</p>
                    <p><strong>Email:</strong> {profileData.email}</p>
                    <hr />
                    <p><strong>Interface Language:</strong> {profileData.preferences.language.toUpperCase()}</p>
                    <p><strong>Solution Detail Level:</strong> {profileData.preferences.solutionDetailLevel}</p>
                    <button
                        onClick={() => setIsEditing(true)}
                        style={{ marginTop: '15px', padding: '8px 16px', cursor: 'pointer' }}
                    >
                        Edit Profile
                    </button>
                </div>
            ) : (
                <div style={{ border: '1px solid #007bff', padding: '15px', borderRadius: '8px' }}>
                    <div style={{ marginBottom: '10px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Name:</label>
                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            style={{ width: '100%', padding: '6px' }}
                        />
                    </div>

                    <div style={{ marginBottom: '10px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Email:</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            style={{ width: '100%', padding: '6px' }}
                        />
                    </div>

                    <div style={{ marginBottom: '10px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Interface Language:</label>
                        <select
                            name="language"
                            value={formData.preferences.language}
                            onChange={handleChange}
                            style={{ width: '100%', padding: '6px' }}
                        >
                            <option value="he">Hebrew (עברית)</option>
                            <option value="en">English (אנגלית)</option>
                            <option value="ru">Russian (Русский)</option>
                        </select>
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Guided Solution Detail Level:</label>
                        <select
                            name="solutionDetailLevel"
                            value={formData.preferences.solutionDetailLevel}
                            onChange={handleChange}
                            style={{ width: '100%', padding: '6px' }}
                        >
                            <option value="basic">Basic (בסיסי)</option>
                            <option value="moderate">Moderate (בינוני)</option>
                            <option value="detailed">Detailed (מפורט)</option>
                        </select>
                    </div>

                    <button onClick={handleSave} style={{ marginRight: '10px', padding: '8px 16px', cursor: 'pointer' }}>Save</button>
                    <button onClick={handleCancel} style={{ padding: '8px 16px', cursor: 'pointer' }}>Cancel</button>
                </div>
            )}
        </div>
    );
};