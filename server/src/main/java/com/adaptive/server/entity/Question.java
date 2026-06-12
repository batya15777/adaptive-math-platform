package com.adaptive.server.entity;

import com.adaptive.server.entity.enums.QuestionStatus;

import javax.persistence.*;

@Entity
@Table(name = "question_archive")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_subject_id", nullable = false)
    private SubSubject subSubject;

    @Column(columnDefinition = "TEXT")
    private String expression;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String solution;

    // Can be a comma-separated string (e.g., "9,8,4,7") or a JSON string
    private String options;

    private String language;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @Enumerated(EnumType.STRING)
    private QuestionStatus status;

    public Question() {
    }

    public Question(SubSubject subSubject, String expression, String correctAnswer, String solution, String options, String language, Integer difficultyLevel, QuestionStatus status) {
        this.subSubject = subSubject;
        this.expression = expression;
        this.correctAnswer = correctAnswer;
        this.solution = solution;
        this.options = options;
        this.language = language;
        this.difficultyLevel = difficultyLevel;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SubSubject getSubSubject() {
        return subSubject;
    }

    public void setSubSubject(SubSubject subSubject) {
        this.subSubject = subSubject;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}