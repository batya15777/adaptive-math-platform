package com.adaptive.server.DTOs;

import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;

public class UpdateProfileRequest {

    private ProfileTheme theme;
    private Language language;
    private Long pictureId;

    public UpdateProfileRequest() {}

    public ProfileTheme getTheme() { return theme; }
    public void setTheme(ProfileTheme theme) { this.theme = theme; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public Long getPictureId() { return pictureId; }
    public void setPictureId(Long pictureId) { this.pictureId = pictureId; }
}
