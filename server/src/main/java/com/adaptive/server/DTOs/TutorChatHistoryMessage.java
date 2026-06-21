package com.adaptive.server.DTOs;

import java.time.LocalDateTime;

/**
 * One saved tutor-chat exchange (the student's message and the tutor's reply),
 * returned when loading a conversation's history. The client expands each exchange
 * into a student turn and a tutor turn for display.
 */
public class TutorChatHistoryMessage {

    private String studentMessage;
    private String tutorResponse;
    private Integer guidanceLevel;
    private String action;
    private LocalDateTime createdAt;

    public TutorChatHistoryMessage() {
    }

    public TutorChatHistoryMessage(String studentMessage, String tutorResponse,
                                   Integer guidanceLevel, String action, LocalDateTime createdAt) {
        this.studentMessage = studentMessage;
        this.tutorResponse = tutorResponse;
        this.guidanceLevel = guidanceLevel;
        this.action = action;
        this.createdAt = createdAt;
    }

    public String getStudentMessage() {
        return studentMessage;
    }

    public void setStudentMessage(String studentMessage) {
        this.studentMessage = studentMessage;
    }

    public String getTutorResponse() {
        return tutorResponse;
    }

    public void setTutorResponse(String tutorResponse) {
        this.tutorResponse = tutorResponse;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
