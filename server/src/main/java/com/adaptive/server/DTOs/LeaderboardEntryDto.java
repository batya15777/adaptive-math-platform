package com.adaptive.server.DTOs;

public class LeaderboardEntryDto {// מחלקה בשביל לא לשוח אובייקט USER שלם כי מכיל סיסמאות מייל מידע רגיש
    private String fullName;
    private Integer totalStars;
    // Index into the client's AVATARS array (UserProfile.pictureId).
    // Nullable: a user may not have a profile row yet — the client falls back to a default icon.
    private Long pictureId;

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
