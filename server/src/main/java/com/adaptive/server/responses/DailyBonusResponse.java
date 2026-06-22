package com.adaptive.server.responses;

// Result of a daily-practice bonus claim. `awarded` is true only when the 100-star
// bonus was actually granted on this call; `totalStars` is always the student's
// up-to-date total so the client can refresh the displayed score / leaderboard.
public class DailyBonusResponse {

    private boolean awarded;
    private int totalStars;
    private int bonus;
    private int correctToday;
    private int goal;

    public DailyBonusResponse() {
    }

    public DailyBonusResponse(boolean awarded, int totalStars, int bonus, int correctToday, int goal) {
        this.awarded = awarded;
        this.totalStars = totalStars;
        this.bonus = bonus;
        this.correctToday = correctToday;
        this.goal = goal;
    }

    public boolean isAwarded() {
        return awarded;
    }

    public void setAwarded(boolean awarded) {
        this.awarded = awarded;
    }

    public int getTotalStars() {
        return totalStars;
    }

    public void setTotalStars(int totalStars) {
        this.totalStars = totalStars;
    }

    public int getBonus() {
        return bonus;
    }

    public void setBonus(int bonus) {
        this.bonus = bonus;
    }

    public int getCorrectToday() {
        return correctToday;
    }

    public void setCorrectToday(int correctToday) {
        this.correctToday = correctToday;
    }

    public int getGoal() {
        return goal;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }
}
