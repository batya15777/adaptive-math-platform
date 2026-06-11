import {useState, useContext} from "react";
import {useProfile} from "../../contexts/ProfileContext.jsx";
import { AuthContext } from "../../context/AuthContext.jsx";

export const ProfileSettings = () => {
    const { profileData, updateProfile } = useProfile();
    const { user } = useContext(AuthContext);

    // Local state for toggling edit mode and handling temporary form data
    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState(profileData);
    const [successMessage, setSuccessMessage] = useState("");

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
        setSuccessMessage("Profile updated successfully!");
        setIsEditing(false);
        setTimeout(() => setSuccessMessage(""), 3000);
    };

    // Discard changes and revert to current context data
    const handleCancel = () => {
        setFormData(profileData);
        setIsEditing(false);
    };

    if (!user) {
        return <div style={{ padding: '20px', textAlign: 'center' }}>Please log in to view your profile.</div>;
    }

    return (
        <div style={{ padding: '20px', maxWidth: '600px', margin: '0 auto', fontFamily: 'Arial, sans-serif' }}>
            <h2 style={{ color: '#333', marginBottom: '20px' }}>Profile Settings</h2>

            {successMessage && (
                <div style={{
                    padding: '12px',
                    marginBottom: '15px',
                    backgroundColor: '#d4edda',
                    color: '#155724',
                    borderRadius: '4px',
                    border: '1px solid #c3e6cb'
                }}>
                    {successMessage}
                </div>
            )}

            {!isEditing ? (
                <div style={{
                    border: '1px solid #ddd',
                    padding: '20px',
                    borderRadius: '8px',
                    backgroundColor: '#f9f9f9'
                }}>
                    <div style={{ marginBottom: '12px' }}>
                        <strong>Full Name:</strong>
                        <p style={{ margin: '4px 0', color: '#555' }}>{profileData.name || 'Not set'}</p>
                    </div>
                    <div style={{ marginBottom: '12px' }}>
                        <strong>Email:</strong>
                        <p style={{ margin: '4px 0', color: '#555' }}>{profileData.email}</p>
                    </div>
                    <hr style={{ margin: '15px 0', borderColor: '#ddd' }} />
                    <div style={{ marginBottom: '12px' }}>
                        <strong>Interface Language:</strong>
                        <p style={{ margin: '4px 0', color: '#555' }}>
                            {profileData.preferences.language === 'he' && 'Hebrew (עברית)'}
                            {profileData.preferences.language === 'en' && 'English'}
                            {profileData.preferences.language === 'ru' && 'Russian (Русский)'}
                        </p>
                    </div>
                    <div style={{ marginBottom: '12px' }}>
                        <strong>Solution Detail Level:</strong>
                        <p style={{ margin: '4px 0', color: '#555' }}>
                            {profileData.preferences.solutionDetailLevel === 'basic' && 'Basic (בסיסי)'}
                            {profileData.preferences.solutionDetailLevel === 'moderate' && 'Moderate (בינוני)'}
                            {profileData.preferences.solutionDetailLevel === 'detailed' && 'Detailed (מפורט)'}
                        </p>
                    </div>
                    <button
                        onClick={() => setIsEditing(true)}
                        style={{
                            marginTop: '15px',
                            padding: '10px 20px',
                            backgroundColor: '#007bff',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontSize: '14px',
                            fontWeight: 'bold'
                        }}
                    >
                        Edit Profile
                    </button>
                </div>
            ) : (
                <div style={{
                    border: '2px solid #007bff',
                    padding: '20px',
                    borderRadius: '8px',
                    backgroundColor: '#f0f8ff'
                }}>
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '6px', fontWeight: 'bold' }}>Full Name:</label>
                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            style={{
                                width: '100%',
                                padding: '8px',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                boxSizing: 'border-box'
                            }}
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '6px', fontWeight: 'bold' }}>Email (Read-only):</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            disabled
                            style={{
                                width: '100%',
                                padding: '8px',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                boxSizing: 'border-box',
                                backgroundColor: '#e9ecef',
                                cursor: 'not-allowed'
                            }}
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '6px', fontWeight: 'bold' }}>Interface Language:</label>
                        <select
                            name="language"
                            value={formData.preferences.language}
                            onChange={handleChange}
                            style={{
                                width: '100%',
                                padding: '8px',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                boxSizing: 'border-box'
                            }}
                        >
                            <option value="he">Hebrew (עברית)</option>
                            <option value="en">English (אנגלית)</option>
                            <option value="ru">Russian (Русский)</option>
                        </select>
                    </div>

                    <div style={{ marginBottom: '20px' }}>
                        <label style={{ display: 'block', marginBottom: '6px', fontWeight: 'bold' }}>Guided Solution Detail Level:</label>
                        <select
                            name="solutionDetailLevel"
                            value={formData.preferences.solutionDetailLevel}
                            onChange={handleChange}
                            style={{
                                width: '100%',
                                padding: '8px',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                boxSizing: 'border-box'
                            }}
                        >
                            <option value="basic">Basic (בסיסי)</option>
                            <option value="moderate">Moderate (בינוני)</option>
                            <option value="detailed">Detailed (מפורט)</option>
                        </select>
                    </div>

                    <div style={{ display: 'flex', gap: '10px' }}>
                        <button
                            onClick={handleSave}
                            style={{
                                flex: 1,
                                padding: '10px',
                                backgroundColor: '#28a745',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: 'bold'
                            }}
                        >
                            Save Changes
                        </button>
                        <button
                            onClick={handleCancel}
                            style={{
                                flex: 1,
                                padding: '10px',
                                backgroundColor: '#6c757d',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: 'bold'
                            }}
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};