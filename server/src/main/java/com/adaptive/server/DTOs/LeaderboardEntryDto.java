package com.adaptive.server.DTOs;

public class LeaderboardEntryDto {// מחלקה בשביל לא לשוח אובייקט USER שלם כי מכיל סיסמאות מייל מידע רגיש
    private String fullName;
    private Integer totalStars;
    // Index into the client's AVATARS array (UserProfile.pictureId). Legacy fallback.
    // Nullable: a user may not have a profile row yet — the client falls back to a default icon.
    private Long pictureId;
    // The student's currently selected avatar (User.selectedAvatarId) — the client renders
    // it from the avatar catalog so the leaderboard shows everyone's up-to-date avatar.
    private String selectedAvatarId;
    // Gender ("male"/"female"/...) — only used to pick a default avatar when the student
    // hasn't chosen one yet.
    private String gender;

    public LeaderboardEntryDto() {
    }

    public LeaderboardEntryDto(String fullName, Integer totalStars) {
        this.fullName = fullName;
        this.totalStars = totalStars;
    }

    public LeaderboardEntryDto(String fullName, Integer totalStars, Long pictureId) {
        this.fullName = fullName;
        this.totalStars = totalStars;
        this.pictureId = pictureId;
    }

    public LeaderboardEntryDto(String fullName, Integer totalStars, Long pictureId, String selectedAvatarId, String gender) {
        this.fullName = fullName;
        this.totalStars = totalStars;
        this.pictureId = pictureId;
        this.selectedAvatarId = selectedAvatarId;
        this.gender = gender;
    }

    public String getSelectedAvatarId() {
        return selectedAvatarId;
    }

    public void setSelectedAvatarId(String selectedAvatarId) {
        this.selectedAvatarId = selectedAvatarId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getTotalStars() {
        return totalStars;
    }

    public void setTotalStars(Integer totalStars) {
        this.totalStars = totalStars;
    }

    public Long getPictureId() {
        return pictureId;
    }

    public void setPictureId(Long pictureId) {
        this.pictureId = pictureId;
    }
}
