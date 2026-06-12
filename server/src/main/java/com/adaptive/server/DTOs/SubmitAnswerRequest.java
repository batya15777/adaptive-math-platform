package com.adaptive.server.DTOs;

public class SubmitAnswerRequest {

    private Long subSubjectId;
    private Long questionId;
    private boolean correct;
    private String questionType;
    private int currentDifficulty;


    public SubmitAnswerRequest() {
    }

    public SubmitAnswerRequest(Long subSubjectId, Long questionId,
                               boolean correct, String questionType, int currentDifficulty) {
        this.subSubjectId = subSubjectId;
        this.questionId = questionId;
        this.correct = correct;
        this.questionType = questionType;
        this.currentDifficulty = currentDifficulty;
    }


    public Long getSubSubjectId() { return subSubjectId; }
    public void setSubSubjectId(Long subSubjectId) { this.subSubjectId = subSubjectId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public int getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(int currentDifficulty) { this.currentDifficulty = currentDifficulty; }
}
