import { useState, useEffect, useContext } from 'react';
import { useProfile } from '../../contexts/useProfile.js';
import { AuthContext } from '../../context/AuthContextSetup.js';
import { useLanguage } from '../../i18n/useLanguage.js';
import { format } from '../../i18n/languages.js';
import { getDashboardData } from '../../service/dashboardApi.js';
import { getProfileSettingsStrings } from './profileSettingsStrings.js';
import { Stars } from '../ui/Stars.jsx';
import { AppTopBar } from '../ui/AppTopBar.jsx';
import { ThemedSelect } from '../ui/ThemedSelect.jsx';
import { AvatarBubble } from './AvatarBubble.jsx';
import { EditProfileModal } from './EditProfileModal.jsx';
import { AvatarStore } from './AvatarStore.jsx';
import { AVATAR_CATALOG, resolveAvatar } from './avatarCatalog.js';
import '../../styles/spaceTokens.css';
import './ProfileSettings.css';

const labelFor = (list, value) => list.find(o => o.value === value)?.label ?? value;

// Profile = identity + preferences (NOT statistics). Themed, gender + language aware.
export const ProfileSettings = () => {
    const { profileData, options, updateProfile, selectAvatar, loading, error } = useProfile();
    const { user } = useContext(AuthContext);
    const { language, dir, locale } = useLanguage();
    const t = getProfileSettingsStrings(language);
    const theme = (profileData.theme || 'LIGHT').toLowerCase();

    // Real server-backed identity (the server returns the full name as `username`).
    const displayName = profileData.name || user?.username || '';
    const genderVal = user?.gender ?? '';
    const gk = genderVal === 'male' ? 'male' : genderVal === 'female' ? 'female' : 'neutral';
    const g = (o) => (o && (o[gk] ?? o.neutral)) || '';

    const [dash, setDash] = useState(null);
    const [starsAfterBuy, setStarsAfterBuy] = useState(null); // updated from the purchase response
    const [editing, setEditing] = useState(false);
    const [storeOpen, setStoreOpen] = useState(null);
    const [okMsg, setOkMsg] = useState('');
    const [saveError, setSaveError] = useState('');

    useEffect(() => {
        let active = true;
        getDashboardData().then(r => { if (active) setDash(r.data); }).catch(() => {});
        return () => { active = false; };
    }, []);

    if (!user) return <div style={{ padding: 24, textAlign: 'center' }}>{t.pleaseLogIn}</div>;

    // Stars: same source of truth as Home (dashboard → student.totalStars = user.totalStars),
    // so the three screens never disagree. `undefined` while loading (we show "…", not 0).
    // After a purchase the server returns the new total → reflect it immediately.
    const stars = starsAfterBuy ?? dash?.student?.totalStars;
    const starsText = stars != null ? stars.toLocaleString(locale) : '…';
    const ownedIds = profileData.ownedAvatarIds || [];
    const level = dash?.overall?.masteryLevel ?? null;
    const genderText = gk === 'male' ? t.genderMale : gk === 'female' ? t.genderFemale : '';

    // Active avatar — same single-source resolution as the leaderboard (selection → pictureId
    // → gender default), so the profile and the board always agree.
    const currentEntry = resolveAvatar({ selectedAvatarId: profileData.selectedAvatarId, pictureId: profileData.pictureId, gender: genderVal });
    const ownedEntries = AVATAR_CATALOG.filter(a => a.price === 0 || ownedIds.includes(a.id));

    const flash = (msg) => { setOkMsg(msg); setTimeout(() => setOkMsg(''), 2500); };

    // Select (and buy first if needed) an avatar. The server deducts stars + records
    // ownership; we just reflect the returned state. Error (e.g. not enough stars) → message.
    const handleSelect = async (entry) => {
        try {
            const data = await selectAvatar(entry.id);
            if (data?.totalStars != null) setStarsAfterBuy(data.totalStars);
            flash(t.saveSuccess);
        } catch {
            setSaveError(t.saveFailed);
        }
    };

    const handleBuy = async (entry) => {
        try {
            const data = await selectAvatar(entry.id);
            if (data?.totalStars != null) setStarsAfterBuy(data.totalStars);
            flash(t.boughtMsg);
        } catch {
            setSaveError(t.saveFailed);
        }
    };

    const handleSaveEdit = async (form) => {
        setSaveError('');
        try {
            // All four persist through the real PUT /profile (theme/language/pictureId on the
            // profile; fullName/gender on the User entity). ProfileContext syncs the auth user,
            // so the card + gender-aware UI update immediately.
            await updateProfile({ theme: form.theme, language: form.language, fullName: form.fullName, gender: form.gender });
            setEditing(false);
            flash(t.saveSuccess);
        } catch {
            setSaveError(t.saveFailed);
        }
    };

    const setPref = (key) => (v) => updateProfile({ [key]: v }).catch(() => setSaveError(t.saveFailed));

    return (
        <div className="mg-space ps-root" data-theme={theme} dir={dir}>
            <Stars />
            <div className="sc-content ps-topbar"><AppTopBar /></div>

            <div className="sc-content ps-inner">
                <header className="ps-head">
                    <h1 className="ps-title">{t.title}</h1>
                    <p className="ps-sub">{t.subtitle}</p>
                </header>

                {loading && <p className="ps-msg">{t.loading}</p>}
                {error && <p className="ps-msg ps-msg--err">{t.loadError}</p>}
                {okMsg && <div className="ps-ok">{okMsg}</div>}
                {saveError && <p className="ps-msg ps-msg--err">{saveError}</p>}

                {/* profile card */}
                <section className="ps-card ps-profile">
                    <div className="ps-avatar-wrap">
                        <span className="ps-avatar-glow" aria-hidden="true" />
                        <AvatarBubble entry={currentEntry} size={96} alt={t.avatarAlt} />
                    </div>
                    <div className="ps-identity">
                        <div className="ps-name">{displayName || t.notSet}</div>
                        <p className="ps-email">{profileData.email}</p>
                        <div className="ps-badges">
                            {level != null && <span className="ps-badge">🏆 {format(t.levelBadge, { level })}</span>}
                            {genderText && <span className="ps-badge">🎓 {genderText}</span>}
                            <span className="ps-badge">⭐ {starsText}</span>
                        </div>
                    </div>
                    <div className="ps-profile-actions">
                        <button type="button" className="sc-btn" onClick={() => setEditing(true)}>👤 {t.editProfile}</button>
                        <button type="button" className="sc-btn sc-btn--ghost" onClick={() => setStoreOpen('recommended')}>🪐 {t.chooseAvatarBtn}</button>
                    </div>
                </section>

                {/* setting cards */}
                <section className="ps-grid">
                    <div className="ps-card ps-setcard">
                        <div className="ps-setcard-h"><span className="ic">🧑</span>{t.sectionAvatar}</div>
                        <div className="ps-mini-avatar">
                            <AvatarBubble entry={currentEntry} size={48} alt={t.avatarAlt} />
                            <span className="ps-mini-name">{t.avatarCurrent}</span>
                        </div>
                        <button type="button" className="sc-btn sc-btn--ghost" onClick={() => setStoreOpen('recommended')}>🪐 {t.changeAvatar}</button>
                    </div>

                    <div className="ps-card ps-setcard">
                        <div className="ps-setcard-h"><span className="ic">🌗</span>{t.theme}</div>
                        <ThemedSelect value={profileData.theme} options={options.themes} onChange={setPref('theme')} icon="🌗" ariaLabel={t.theme} />
                        <span className="ps-setcard-sub">{labelFor(options.themes, profileData.theme)}</span>
                    </div>

                    <div className="ps-card ps-setcard">
                        <div className="ps-setcard-h"><span className="ic">🌐</span>{t.language}</div>
                        <ThemedSelect value={profileData.language} options={options.languages} onChange={setPref('language')} icon="🌐" ariaLabel={t.language} />
                        <span className="ps-setcard-sub">{labelFor(options.languages, profileData.language)}</span>
                    </div>

                    <div className="ps-card ps-setcard">
                        <div className="ps-setcard-h"><span className="ic">🔔</span>{t.sectionNotifications}</div>
                        <NotificationsToggle hint={t.notificationsHint} />
                    </div>

                    <div className="ps-card ps-setcard">
                        <div className="ps-setcard-h"><span className="ic">⭐</span>{t.sectionStars}</div>
                        <div className="ps-stars-big">{starsText} ⭐</div>
                        <span className="ps-setcard-sub">{t.starsHint}</span>
                    </div>

                    <div className="ps-card ps-setcard">
                        <div className="ps-setcard-h"><span className="ic">🪐</span>{t.sectionMyAvatars}</div>
                        <div className="ps-avatars-row">
                            {ownedEntries.slice(0, 4).map(a => <AvatarBubble key={a.id} entry={a} size={40} alt={t.avatarNames?.[a.nameKey] || ''} />)}
                        </div>
                        <button type="button" className="ps-link" onClick={() => setStoreOpen('mine')}>{g(t.viewCollection)}</button>
                    </div>

                    {level != null && (
                        <div className="ps-card ps-setcard">
                            <div className="ps-setcard-h"><span className="ic">🏆</span>{t.sectionLastAchievement}</div>
                            <div className="ps-ach">
                                <span className="ps-ach-medal" aria-hidden="true">🌟</span>
                                <div>
                                    <div className="ps-ach-name">{t.achievementName}</div>
                                    <div className="ps-ach-desc">{format(t.achievementDesc, { level })}</div>
                                </div>
                            </div>
                        </div>
                    )}
                </section>
            </div>

            {editing && (
                <EditProfileModal
                    profileData={profileData}
                    options={options}
                    initialName={displayName}
                    initialGender={genderVal}
                    currentEntry={currentEntry}
                    onSave={handleSaveEdit}
                    onClose={() => setEditing(false)}
                    onChangeAvatar={() => { setEditing(false); setStoreOpen('recommended'); }}
                    t={t}
                    loading={loading}
                />
            )}
            {saveError && editing && <p className="ps-msg ps-msg--err">{saveError}</p>}

            {storeOpen && (
                <AvatarStore
                    gk={gk}
                    stars={stars ?? 0}
                    selectedId={currentEntry?.id}
                    ownedIds={ownedIds}
                    onSelect={handleSelect}
                    onBuy={handleBuy}
                    onClose={() => setStoreOpen(null)}
                    initialTab={storeOpen}
                    t={t}
                    g={g}
                    locale={locale}
                />
            )}
        </div>
    );
};

// Placeholder notifications toggle — local only. TODO(backend): persist a real preference.
const NotificationsToggle = ({ hint }) => {
    const [on, setOn] = useState(true);
    return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
            <span className="ps-setcard-sub">{hint}</span>
            <button
                type="button"
                onClick={() => setOn(v => !v)}
                aria-pressed={on}
                style={{
                    width: 46, height: 26, flex: 'none', borderRadius: 999, border: '1px solid var(--mg-bd)', cursor: 'pointer',
                    background: on ? 'var(--mg-cta)' : 'var(--mg-s2)', position: 'relative', transition: 'background .15s ease',
                }}
            >
                <span style={{
                    position: 'absolute', top: 2, insetInlineStart: on ? 22 : 2, width: 20, height: 20, borderRadius: '50%',
                    background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,.3)', transition: 'inset-inline-start .15s ease',
                }} />
            </button>
        </div>
    );
};
