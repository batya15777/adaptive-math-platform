import { useState } from 'react';
import { ThemedSelect } from '../ui/ThemedSelect.jsx';
import { AvatarBubble } from './AvatarBubble.jsx';

// Edit-profile modal. Full name + gender + theme + language all persist through the real
// PUT /profile (fullName/gender are applied to the User entity server-side). Email stays
// read-only (changing it needs verification we don't have).
//   initialName / initialGender   current values (override-aware)
//   profileData, options          from useProfile()
//   currentEntry                  catalog entry for the active avatar
//   onSave({ fullName, gender, theme, language }) / onClose / onChangeAvatar
//   t, loading
export function EditProfileModal({ initialName, initialGender, profileData, options, currentEntry, onSave, onClose, onChangeAvatar, t, loading }) {
    const [form, setForm] = useState({
        fullName: initialName || '',
        gender: initialGender || '',
        theme: profileData.theme,
        language: profileData.language,
    });
    const set = (k) => (v) => setForm(prev => ({ ...prev, [k]: v }));

    // Gender stays in the project's existing format: 'female' | 'male' | '' (unset/other).
    const genderOptions = [
        { value: 'female', label: t.genderFemale },
        { value: 'male', label: t.genderMale },
        { value: '', label: t.genderNeutral },
    ];

    return (
        <div className="ps-overlay" role="dialog" aria-modal="true" aria-label={t.editTitle} onClick={onClose}>
            <div className="ps-modal" onClick={(e) => e.stopPropagation()}>
                <div className="ps-modal-head">
                    <h2 className="ps-modal-title">{t.editTitle}</h2>
                    <button type="button" className="ps-modal-x" onClick={onClose} aria-label={t.cancel}>✕</button>
                </div>

                <div className="ps-edit-avatar">
                    <AvatarBubble entry={currentEntry} size={88} alt={t.avatarAlt} />
                    <button type="button" className="ps-link" onClick={onChangeAvatar}>🪐 {t.changeAvatar}</button>
                </div>

                {/* editable name */}
                <div className="ps-field">
                    <label htmlFor="ps-name">{t.fieldName}</label>
                    <input
                        id="ps-name"
                        className="ps-input"
                        value={form.fullName}
                        onChange={(e) => set('fullName')(e.target.value)}
                        placeholder={t.fieldName}
                        autoComplete="name"
                    />
                </div>

                {/* read-only email (no API to change it) */}
                <div className="ps-field">
                    <label>{t.fieldEmail} <span className="ro">({t.readonlyNote})</span></label>
                    <input className="ps-input" value={profileData.email || ''} disabled />
                </div>

                {/* editable gender */}
                <div className="ps-field">
                    <label>{t.fieldGender}</label>
                    <ThemedSelect value={form.gender} options={genderOptions} onChange={set('gender')} ariaLabel={t.fieldGender} />
                </div>

                {/* editable preferences */}
                <div className="ps-field">
                    <label>{t.theme}</label>
                    <ThemedSelect value={form.theme} options={options.themes} onChange={set('theme')} ariaLabel={t.theme} />
                </div>
                <div className="ps-field">
                    <label>{t.language}</label>
                    <ThemedSelect value={form.language} options={options.languages} onChange={set('language')} ariaLabel={t.language} />
                </div>

                <div className="ps-modal-actions">
                    <button type="button" className="sc-btn sc-btn--ghost" onClick={onClose} disabled={loading}>{t.cancel}</button>
                    <button type="button" className="sc-btn" onClick={() => onSave(form)} disabled={loading}>
                        {loading ? t.saving : t.saveChanges}
                    </button>
                </div>
            </div>
        </div>
    );
}
