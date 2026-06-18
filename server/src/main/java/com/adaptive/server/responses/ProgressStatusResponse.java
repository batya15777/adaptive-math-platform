package com.adaptive.server.responses;

public class ProgressStatusResponse extends BasicResponse {//תמונת מצב כללית של תלמיד
    private int currentLevel;
    private String spaceshipStatus;
    private boolean levelUp;//האם עלה רמה
    private boolean inIntermediateLevel;//האם הוא תקוע
    private String recommendedQuestionType;
    private String weaknessType;//חולשה של תלמיד
    private int correctLast10;
    private int correctLast30;
    private long totalAttempts;
    private boolean bonusQuestionTriggered;// זה יהיה TRUE שתלמיד עלה רמה וצריך לעשות שאלת בונוס לכל רמה
    private int totalStars;// זה סכ"ה כמות כוכבים של תלמיד

    // ── question-game fields ─────────────────────────────────────────────
    private boolean answerCorrect;      // was the just-submitted answer correct
    private String concluded;           // "SOLVED" / "FAILED" / null (still retrying)
    private boolean inSubLevel;         // easier-practice mode (frozen level, frozen bar)
    private int levelProgressCurrent;   // correct answers in the current level-up window
    private int levelProgressTarget;    // window threshold to level up (LEVEL_UP_THRESHOLD)

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

    public boolean isAnswerCorrect() {
        return answerCorrect;
    }

    public void setAnswerCorrect(boolean answerCorrect) {
        this.answerCorrect = answerCorrect;
    }

    public String getConcluded() {
        return concluded;
    }

    public void setConcluded(String concluded) {
        this.concluded = concluded;
    }

    public boolean isInSubLevel() {
        return inSubLevel;
    }

    public void setInSubLevel(boolean inSubLevel) {
        this.inSubLevel = inSubLevel;
    }

    public int getLevelProgressCurrent() {
        return levelProgressCurrent;
    }

    public void setLevelProgressCurrent(int levelProgressCurrent) {
        this.levelProgressCurrent = levelProgressCurrent;
    }

    public int getLevelProgressTarget() {
        return levelProgressTarget;
    }

    public void setLevelProgressTarget(int levelProgressTarget) {
        this.levelProgressTarget = levelProgressTarget;
    }
}
