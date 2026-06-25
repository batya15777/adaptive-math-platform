package com.adaptive.server.responses;

public class QuestionResponse extends BasicResponse{
    private Long questionId;
    private String expression;
    private String correctAnswer;
    private String solution;
    private String options;
    private int difficultyLevel;
    private Long subSubjectId;
    private String recommendedQuestionType;
    /** False for normal questions; BonusQuestionResponse overrides this to true. */
    private boolean bonus;
    /** Stars awarded on correct answer. 0 for normal questions; 50 for bonus. */
    private int starsReward;
    /**
     * Daily Practice only: true means "this sub-subject isn't eligible for this student's
     * daily mix (e.g. polynomial/algebra for an early grade) — skip it and try the next topic".
     * Always false for Regular Practice, so that flow is unaffected.
     */
    private boolean skipped;

    public QuestionResponse() {
        super();
    }

    /**
     * Sentinel response telling the Daily Practice client to skip this topic without
     * generating a question (no level/progress/archive side-effects on the server).
     */
    public static QuestionResponse skipped() {
        QuestionResponse response = new QuestionResponse();
        response.setSuccess(true);
        response.setSkipped(true);
        return response;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Long getSubSubjectId() {
        return subSubjectId;
    }

    public void setSubSubjectId(Long subSubjectId) {
        this.subSubjectId = subSubjectId;
    }

    public String getRecommendedQuestionType() {
        return recommendedQuestionType;
    }

    public void setRecommendedQuestionType(String recommendedQuestionType) {
        this.recommendedQuestionType = recommendedQuestionType;
    }

    public boolean isBonus() {
        return bonus;
    }

    public void setBonus(boolean bonus) {
        this.bonus = bonus;
    }

    public int getStarsReward() {
        return starsReward;
    }

    public void setStarsReward(int starsReward) {
        this.starsReward = starsReward;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}
