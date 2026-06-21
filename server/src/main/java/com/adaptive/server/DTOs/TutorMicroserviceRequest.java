package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TutorMicroserviceRequest {

    private TutorQuestionContext question;
    private TutorStudentContext student;

    @JsonProperty("student_message")
    private String studentMessage;

    @JsonProperty("conversation_history")
    private List<ConversationMessage> conversationHistory;

    @JsonProperty("guidance_level")
    private Integer guidanceLevel;

    public TutorMicroserviceRequest() {
    }

    public TutorQuestionContext getQuestion() {
        return question;
    }

    public void setQuestion(TutorQuestionContext question) {
        this.question = question;
    }

    public TutorStudentContext getStudent() {
        return student;
    }

    public void setStudent(TutorStudentContext student) {
        this.student = student;
    }

    public String getStudentMessage() {
        return studentMessage;
    }

    public void setStudentMessage(String studentMessage) {
        this.studentMessage = studentMessage;
    }

    public List<ConversationMessage> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ConversationMessage> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public Integer getGuidanceLevel() {
        return guidanceLevel;
    }

    public void setGuidanceLevel(Integer guidanceLevel) {
        this.guidanceLevel = guidanceLevel;
    }
}
