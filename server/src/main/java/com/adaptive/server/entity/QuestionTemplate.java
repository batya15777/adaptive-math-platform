package com.adaptive.server.entity;

import javax.persistence.*;

@Entity
@Table(name = "question_template")
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_subject_id", nullable = false)
    private SubSubject subSubject;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @Column(name = "sub_subject_level")
    private Integer subSubjectLevel;

    @Column(columnDefinition = "TEXT")
    private String expression;

    public QuestionTemplate() {
    }

    public QuestionTemplate(SubSubject subSubject, String expression) {
        this.subSubject = subSubject;
        this.expression = expression;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
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
}
