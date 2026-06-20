package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** All attempts for a single student, grouped under their user id. */
public class MLStudentFeaturesDto {

    @JsonProperty("user_id")
    private Long userId;

    private List<MLAttemptFeatureDto> attempts;

    public MLStudentFeaturesDto() {
    }

    public MLStudentFeaturesDto(Long userId, List<MLAttemptFeatureDto> attempts) {
        this.userId = userId;
        this.attempts = attempts;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<MLAttemptFeatureDto> getAttempts() { return attempts; }
    public void setAttempts(List<MLAttemptFeatureDto> attempts) { this.attempts = attempts; }
}
