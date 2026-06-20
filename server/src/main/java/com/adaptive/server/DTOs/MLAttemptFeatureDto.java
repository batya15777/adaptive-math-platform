package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * One exercise attempt as sent to the Python clustering microservice.
 * Field names are serialised to snake_case to match the Pydantic schema.
 */
public class MLAttemptFeatureDto {

    @JsonProperty("error_pattern")
    private String errorPattern;

    @JsonProperty("question_type")
    private String questionType;

    @JsonProperty("difficulty_level")
    private Integer difficultyLevel;

    @JsonProperty("is_correct")
    private Boolean isCorrect;

    @JsonProperty("user_answer")
    private String userAnswer;

    @JsonProperty("answered_at")
    private LocalDateTime answeredAt;

    public MLAttemptFeatureDto() {
    }

    public MLAttemptFeatureDto(String errorPattern, String questionType, Integer difficultyLevel,
                               Boolean isCorrect, String userAnswer, LocalDateTime answeredAt) {
        this.errorPattern = errorPattern;
        this.questionType = questionType;
        this.difficultyLevel = difficultyLevel;
        this.isCorrect = isCorrect;
        this.userAnswer = userAnswer;
        this.answeredAt = answeredAt;
    }

    public String getErrorPattern() { return errorPattern; }
    public void setErrorPattern(String errorPattern) { this.errorPattern = errorPattern; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
