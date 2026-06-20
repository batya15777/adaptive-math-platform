package com.adaptive.server.DTOs;

public class TutorChatResponse {

    private String message;

    private Integer guidanceLevel;

    private String action;

    public TutorChatResponse() {
    }

    public TutorChatResponse(String message, Integer guidanceLevel, String action) {
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
