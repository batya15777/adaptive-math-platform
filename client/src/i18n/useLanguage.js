import { useProfile } from "../contexts/useProfile.js";
import { toLangCode, isRtl, localeFor } from "./languages.js";

// Single integration point between the persisted profile language and the UI.
// Language is read from the profile = one source of truth across the whole app.
// Returns the active ISO code plus derived direction/locale so components don't
// re-derive them.
export const useLanguage = () => {
    const { profileData } = useProfile();
    const language = toLangCode(profileData?.language);
    return {
        language,
        dir: isRtl(language) ? "rtl" : "ltr",
        locale: localeFor(language),
        isRtl: isRtl(language),
    };
};
