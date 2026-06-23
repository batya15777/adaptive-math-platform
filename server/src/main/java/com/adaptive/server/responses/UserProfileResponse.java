package com.adaptive.server.responses;

import com.adaptive.server.entity.User;
import com.adaptive.server.entity.UserProfile;
import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;

import java.util.ArrayList;
import java.util.List;

public class UserProfileResponse {

    private boolean success;
    private ProfileTheme theme;
    private Language language;
    private long pictureId;
    private int totalStars;
    private String selectedAvatarId;
    private List<String> ownedAvatarIds;

    public UserProfileResponse() {}

    public UserProfileResponse(UserProfile profile, User user) {
        this.success          = true;
        this.theme            = profile.getTheme();
        this.language         = profile.getLanguage();
        this.pictureId        = profile.getPictureId();
        this.totalStars       = user.getTotalStars();
        this.selectedAvatarId = user.getSelectedAvatarId();
        this.ownedAvatarIds   = new ArrayList<>(user.getOwnedAvatarIds());
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ProfileTheme getTheme() { return theme; }
    public void setTheme(ProfileTheme theme) { this.theme = theme; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public long getPictureId() { return pictureId; }
    public void setPictureId(long pictureId) { this.pictureId = pictureId; }

    public int getTotalStars() { return totalStars; }
    public void setTotalStars(int totalStars) { this.totalStars = totalStars; }

    public String getSelectedAvatarId() { return selectedAvatarId; }
    public void setSelectedAvatarId(String selectedAvatarId) { this.selectedAvatarId = selectedAvatarId; }

    public List<String> getOwnedAvatarIds() { return ownedAvatarIds; }
    public void setOwnedAvatarIds(List<String> ownedAvatarIds) { this.ownedAvatarIds = ownedAvatarIds; }
}
