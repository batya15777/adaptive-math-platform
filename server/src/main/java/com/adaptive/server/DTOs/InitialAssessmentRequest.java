package com.adaptive.server.DTOs;

public class InitialAssessmentRequest {//נתונים של תלמיד בשאלון הגדרת רמה לאחר הרשמה
    private Long subSubjectId;
    private Integer grade;
    private String confidenceLevel; // "EASY", "MEDIUM", "HARD"
    private String language;

    public InitialAssessmentRequest() {
    }

    public InitialAssessmentRequest(Long subSubjectId, Integer grade, String confidenceLevel, String language) {
        this.subSubjectId = subSubjectId;
        this.grade = grade;
        this.confidenceLevel = confidenceLevel;
        this.language = language;
    }

    public Long getSubSubjectId() {
        return subSubjectId;
    }

    public void setSubSubjectId(Long subSubjectId) {
        this.subSubjectId = subSubjectId;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
