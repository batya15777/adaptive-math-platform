package com.adaptive.server.DTOs;

public class RevealSolutionRequest {

    private Long subSubjectId;
    private Long questionId;

    public RevealSolutionRequest() {
    }

    public Long getSubSubjectId() { return subSubjectId; }
    public void setSubSubjectId(Long subSubjectId) { this.subSubjectId = subSubjectId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
}
