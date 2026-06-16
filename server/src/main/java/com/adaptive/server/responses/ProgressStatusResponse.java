package com.adaptive.server.responses;

public class ProgressStatusResponse extends BasicResponse {
    private int currentLevel;
    private String spaceshipStatus;
    private boolean levelUp;
    private boolean inIntermediateLevel;
    private String recommendedQuestionType;
    private String weaknessType;
    private int correctLast10;
    private int correctLast30;
    private long totalAttempts;
    /** True when the student just levelled up and a bonus question is ready to be presented. */
    private boolean bonusQuestionTriggered;
    /** The user's total accumulated stars across all subjects (updated after each submission). */
    private int totalStars;

    public ProgressStatusResponse() {
        super();
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getSpaceshipStatus() {
        return spaceshipStatus;
    }

    public void setSpaceshipStatus(String spaceshipStatus) {
        this.spaceshipStatus = spaceshipStatus;
    }

    public boolean isLevelUp() {
        return levelUp;
    }

    public void setLevelUp(boolean levelUp) {
        this.levelUp = levelUp;
    }

    public boolean isInIntermediateLevel() {
        return inIntermediateLevel;
    }

    public void setInIntermediateLevel(boolean inIntermediateLevel) {
        this.inIntermediateLevel = inIntermediateLevel;
    }

    public String getRecommendedQuestionType() {
        return recommendedQuestionType;
    }

    public void setRecommendedQuestionType(String recommendedQuestionType) {
        this.recommendedQuestionType = recommendedQuestionType;
    }

    public String getWeaknessType() {
        return weaknessType;
    }

    public void setWeaknessType(String weaknessType) {
        this.weaknessType = weaknessType;
    }

    public int getCorrectLast10() {
        return correctLast10;
    }

    public void setCorrectLast10(int correctLast10) {
        this.correctLast10 = correctLast10;
    }

    public int getCorrectLast30() {
        return correctLast30;
    }

    public void setCorrectLast30(int correctLast30) {
        this.correctLast30 = correctLast30;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public boolean isBonusQuestionTriggered() {
        return bonusQuestionTriggered;
    }

    public void setBonusQuestionTriggered(boolean bonusQuestionTriggered) {
        this.bonusQuestionTriggered = bonusQuestionTriggered;
    }

    public int getTotalStars() {
        return totalStars;
    }

    public void setTotalStars(int totalStars) {
        this.totalStars = totalStars;
    }
}
