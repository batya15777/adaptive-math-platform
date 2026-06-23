package com.adaptive.server.DTOs;

import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;

public class UpdateProfileRequest {

    private ProfileTheme theme;
    private Language language;
    private Long pictureId;
    private String fullName;   // identity fields live on the User entity (see UserProfileService)
    private String gender;     // "male" | "female" | "" (matches registration)

    public UpdateProfileRequest() {}

    public ProfileTheme getTheme() { return theme; }
    public void setTheme(ProfileTheme theme) { this.theme = theme; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public Long getPictureId() { return pictureId; }
    public void setPictureId(Long pictureId) { this.pictureId = pictureId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
