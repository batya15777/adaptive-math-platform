package com.adaptive.server.responses;

import com.adaptive.server.entity.UserProfile;
import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;

public class UserProfileResponse {

    private boolean success;
    private ProfileTheme theme;
    private Language language;
    private long pictureId;

    public UserProfileResponse() {}

    public UserProfileResponse(UserProfile profile) {
        this.success   = true;
        this.theme     = profile.getTheme();
        this.language  = profile.getLanguage();
        this.pictureId = profile.getPictureId();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ProfileTheme getTheme() { return theme; }
    public void setTheme(ProfileTheme theme) { this.theme = theme; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public long getPictureId() { return pictureId; }
    public void setPictureId(long pictureId) { this.pictureId = pictureId; }
}
