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

    public QuestionResponse() {
        super();
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
}
