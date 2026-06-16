package com.adaptive.server.DTOs;

public class SubmitAnswerRequest {

    private Long subSubjectId;
    private Long questionId;
    private String userAnswer;
    private String questionType;
    private int currentDifficulty;


    public SubmitAnswerRequest() {
    }

    public SubmitAnswerRequest(Long subSubjectId, Long questionId,
                               String userAnswer, String questionType, int currentDifficulty) {
        this.subSubjectId = subSubjectId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.questionType = questionType;
        this.currentDifficulty = currentDifficulty;
    }


    public Long getSubSubjectId() { return subSubjectId; }
    public void setSubSubjectId(Long subSubjectId) { this.subSubjectId = subSubjectId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public int getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(int currentDifficulty) { this.currentDifficulty = currentDifficulty; }
}
