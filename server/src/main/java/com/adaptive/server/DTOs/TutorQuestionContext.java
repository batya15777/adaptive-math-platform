package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TutorQuestionContext {

    @JsonProperty("question_id")
    private Long questionId;

    private String expression;

    @JsonProperty("correct_answer")
    private String correctAnswer;

    @JsonProperty("solution_steps")
    private List<String> solutionSteps;

    private List<String> options;
    private String subject;

    @JsonProperty("sub_subject")
    private String subSubject;

    @JsonProperty("difficulty_level")
    private Integer difficultyLevel;

    private String language;
    private String source;

    public TutorQuestionContext() {
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

    public List<String> getSolutionSteps() {
        return solutionSteps;
    }

    public void setSolutionSteps(List<String> solutionSteps) {
        this.solutionSteps = solutionSteps;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubSubject() {
        return subSubject;
    }

    public void setSubSubject(String subSubject) {
        this.subSubject = subSubject;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
