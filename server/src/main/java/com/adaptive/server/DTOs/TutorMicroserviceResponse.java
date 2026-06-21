package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TutorMicroserviceResponse {

    private String message;

    @JsonProperty("guidance_level")
    private Integer guidanceLevel;

    private String action;

    public TutorMicroserviceResponse() {
    }

    public TutorMicroserviceResponse(String message, Integer guidanceLevel, String action) {
        this.message = message;
        this.guidanceLevel = guidanceLevel;
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getGuidanceLevel() {
        return guidanceLevel;
    }

    public void setGuidanceLevel(Integer guidanceLevel) {
        this.guidanceLevel = guidanceLevel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
