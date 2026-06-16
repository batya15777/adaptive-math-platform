package com.adaptive.server.DTOs;

public class LeaderboardEntryDto {// מחלקה בשביל לא לשוח אובייקט USER שלם כי מכיל סיסמאות מייל מידע רגיש
    private String fullName;
    private Integer totalStars;

    public LeaderboardEntryDto() {
    }

    public LeaderboardEntryDto(String fullName, Integer totalStars) {
        this.fullName = fullName;
        this.totalStars = totalStars;
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
}
