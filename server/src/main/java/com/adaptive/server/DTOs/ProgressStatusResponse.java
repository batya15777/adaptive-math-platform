package com.adaptive.server.DTOs;

public class ProgressStatusResponse {

    private int currentLevel;
    private String spaceshipStatus;
    private boolean levelUp;
    private boolean success;

     //True when the student is currently in an intermediate/remedial state
     //spaceship is stopped and easier questions are being served.
     private boolean inIntermediateLevel;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    // The question type the generator should focus on next.
     // Null when there is no weakness detected.
     // Example: "CARRYING_ADDITION"
    private String recommendedQuestionType;


     // The question type where the student is weakest (> 50% error rate in last 15).
     // Null if no clear weakness is detected.
    private String weaknessType;

    private String message;

    // How many of the last 10 answers were correct (0–10).
    private int correctLast10;

    // How many of the last 30 answers were correct (0–30).
    private int correctLast30;

    // Total number of attempts by this student in this sub-subject.
    private long totalAttempts;


    public ProgressStatusResponse() {
    }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

    public String getSpaceshipStatus() { return spaceshipStatus; }
    public void setSpaceshipStatus(String spaceshipStatus) { this.spaceshipStatus = spaceshipStatus; }

    public boolean isLevelUp() { return levelUp; }
    public void setLevelUp(boolean levelUp) { this.levelUp = levelUp; }

    public boolean isInIntermediateLevel() { return inIntermediateLevel; }
    public void setInIntermediateLevel(boolean inIntermediateLevel) {
        this.inIntermediateLevel = inIntermediateLevel;
    }

    public String getRecommendedQuestionType() { return recommendedQuestionType; }
    public void setRecommendedQuestionType(String recommendedQuestionType) {
        this.recommendedQuestionType = recommendedQuestionType;
    }

    public String getWeaknessType() { return weaknessType; }
    public void setWeaknessType(String weaknessType) { this.weaknessType = weaknessType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getCorrectLast10() { return correctLast10; }
    public void setCorrectLast10(int correctLast10) { this.correctLast10 = correctLast10; }

    public int getCorrectLast30() { return correctLast30; }
    public void setCorrectLast30(int correctLast30) { this.correctLast30 = correctLast30; }

    public long getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(long totalAttempts) { this.totalAttempts = totalAttempts; }
}
