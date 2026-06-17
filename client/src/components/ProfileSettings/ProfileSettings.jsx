import { useState, useContext, useEffect } from 'react';
import { useProfile } from '../../contexts/useProfile.js';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { AVATARS } from '../../assets/avatars/index.js';

const LANGUAGE_LABELS = {
    HEBREW:  'Hebrew (עברית)',
    ENGLISH: 'English',
    RUSSIAN: 'Russian (Русский)',
};

const THEME_LABELS = {
    LIGHT: 'Light',
    DARK:  'Dark',
};

export const ProfileSettings = () => {
    const { profileData, updateProfile, loading, error } = useProfile();
    const { user } = useContext(AuthContext);

    const [isEditing, setIsEditing]    = useState(false);
    const [formData,  setFormData]     = useState({});
    const [successMsg, setSuccessMsg]  = useState('');
    const [saveError,  setSaveError]   = useState('');

    useEffect(() => {
        setFormData({
            theme:     profileData.theme,
            language:  profileData.language,
            pictureId: profileData.pictureId,
        });
    }, [profileData]);

    if (!user) {
        return <div style={{ padding: '20px', textAlign: 'center' }}>Please log in to view your profile.</div>;
    }

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleAvatarSelect = (index) => {
        setFormData(prev => ({ ...prev, pictureId: index }));
    };

    const handleSave = async () => {
        setSaveError('');
        try {
            await updateProfile(formData);
            setSuccessMsg('Profile updated successfully!');
            setIsEditing(false);
            setTimeout(() => setSuccessMsg(''), 3000);
        } catch {
            setSaveError('Could not save. Please try again.');
        }
    };

    const handleCancel = () => {
        setFormData({
            theme:     profileData.theme,
            language:  profileData.language,
            pictureId: profileData.pictureId,
        });
        setSaveError('');
        setIsEditing(false);
    };

    return (
        <div style={{ padding: '20px', maxWidth: '600px', margin: '0 auto', fontFamily: 'Arial, sans-serif' }}>
            <h2 style={{ color: '#333', marginBottom: '20px' }}>Profile Settings</h2>

            {loading && <p style={{ color: '#888' }}>Loading...</p>}
            {error   && <p style={{ color: '#dc3545' }}>{error}</p>}

            {successMsg && (
                <div style={{ padding: '12px', marginBottom: '15px', backgroundColor: '#d4edda', color: '#155724', borderRadius: '4px', border: '1px solid #c3e6cb' }}>
                    {successMsg}
                </div>
            )}

            {!isEditing ? (
                /* ── View mode ─────────────────────────────────────────── */
                <div style={{ border: '1px solid #ddd', padding: '20px', borderRadius: '8px', backgroundColor: '#f9f9f9' }}>

                    {/* Avatar */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
                        <img
                            src={AVATARS[profileData.pictureId] ?? AVATARS[0]}
                            alt="Your avatar"
                            style={{ width: '72px', height: '72px', borderRadius: '50%', objectFit: 'cover', border: '2px solid #ddd' }}
                        />
                        <div>
                            <strong style={{ fontSize: '18px' }}>{profileData.name || 'Not set'}</strong>
                            <p style={{ margin: '4px 0', color: '#888', fontSize: '14px' }}>{profileData.email}</p>
                        </div>
                    </div>

                    <hr style={{ margin: '15px 0', borderColor: '#ddd' }} />
                    <Field label="Theme">{THEME_LABELS[profileData.theme]}</Field>
                    <Field label="Language">{LANGUAGE_LABELS[profileData.language]}</Field>

                    <button onClick={() => setIsEditing(true)} style={btn('#007bff')}>
                        Edit Profile
                    </button>
                </div>
            ) : (
                /* ── Edit mode ─────────────────────────────────────────── */
                <div style={{ border: '2px solid #007bff', padding: '20px', borderRadius: '8px', backgroundColor: '#f0f8ff' }}>

                    {/* Avatar picker */}
                    <div style={{ marginBottom: '20px' }}>
                        <label style={labelStyle}>Choose Avatar</label>
                        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginTop: '8px' }}>
                            {AVATARS.map((src, index) => {
                                const selected = formData.pictureId === index;
                                return (
                                    <img
                                        key={index}
                                        src={src}
                                        alt={`Avatar ${index}`}
                                        onClick={() => handleAvatarSelect(index)}
                                        style={{
                                            width: '64px',
                                            height: '64px',
                                            borderRadius: '50%',
                                            objectFit: 'cover',
                                            cursor: 'pointer',
                                            border: selected ? '3px solid #007bff' : '3px solid transparent',
                                            boxShadow: selected ? '0 0 0 2px #007bff55' : 'none',
                                            transition: 'border 0.15s, box-shadow 0.15s',
                                        }}
                                    />
                                );
                            })}
                        </div>
                    </div>

                    {/* Read-only identity */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={labelStyle}>Full Name (read-only)</label>
                        <input value={profileData.name} disabled style={{ ...inputStyle, backgroundColor: '#e9ecef', cursor: 'not-allowed' }} />
                    </div>
                    <div style={{ marginBottom: '15px' }}>
                        <label style={labelStyle}>Email (read-only)</label>
                        <input value={profileData.email} disabled style={{ ...inputStyle, backgroundColor: '#e9ecef', cursor: 'not-allowed' }} />
                    </div>

                    {/* Theme */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={labelStyle}>Theme</label>
                        <select name="theme" value={formData.theme} onChange={handleChange} style={inputStyle}>
                            <option value="LIGHT">Light</option>
                            <option value="DARK">Dark</option>
                        </select>
                    </div>

                    {/* Language */}
                    <div style={{ marginBottom: '20px' }}>
                        <label style={labelStyle}>Language</label>
                        <select name="language" value={formData.language} onChange={handleChange} style={inputStyle}>
                            <option value="HEBREW">Hebrew (עברית)</option>
                            <option value="ENGLISH">English</option>
                            <option value="RUSSIAN">Russian (Русский)</option>
                        </select>
                    </div>

                    {saveError && <p style={{ color: '#dc3545', marginBottom: '10px' }}>{saveError}</p>}

                    <div style={{ display: 'flex', gap: '10px' }}>
                        <button onClick={handleSave}   disabled={loading} style={btn('#28a745')}>
                            {loading ? 'Saving...' : 'Save Changes'}
                        </button>
                        <button onClick={handleCancel} disabled={loading} style={btn('#6c757d')}>
                            Cancel
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

// ── small helpers ────────────────────────────────────────────────────────────

const Field = ({ label, children }) => (
    <div style={{ marginBottom: '12px' }}>
        <strong>{label}:</strong>
        <p style={{ margin: '4px 0', color: '#555' }}>{children}</p>
    </div>
);

const labelStyle = { display: 'block', marginBottom: '6px', fontWeight: 'bold' };

const inputStyle = {
    width: '100%', padding: '8px', border: '1px solid #ccc',
    borderRadius: '4px', boxSizing: 'border-box',
};

const btn = (bg) => ({
    flex: 1, padding: '10px', backgroundColor: bg, color: 'white',
    border: 'none', borderRadius: '4px', cursor: 'pointer',
    fontSize: '14px', fontWeight: 'bold',
});
