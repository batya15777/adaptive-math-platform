package com.adaptive.server.DTOs;

public class TutorChatRequest {

    private Long questionId;
    private String message;

    public TutorChatRequest() {
    }

    public TutorChatRequest(Long questionId, String message) {
        this.questionId = questionId;
        this.message = message;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
