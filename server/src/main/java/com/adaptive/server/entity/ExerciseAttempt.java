package com.adaptive.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_attempts", indexes = {@Index(name = "idx_attempt_user_subject_time",
               columnList = "user_id, sub_subject_id, answered_at DESC")
})
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_subject_id", nullable = false)
    private SubSubject subSubject;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "is_correct", nullable = false)//if student answer correctly
    private Boolean isCorrect;

    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;


    public ExerciseAttempt() {
    }

    public ExerciseAttempt(User user, SubSubject subSubject, Long questionId,
                           Boolean isCorrect, Integer difficultyLevel,
                           String questionType, LocalDateTime answeredAt){
        this.user = user;
        this.subSubject = subSubject;
        this.questionId = questionId;
        this.isCorrect = isCorrect;
        this.difficultyLevel = difficultyLevel;
        this.questionType = questionType;
        this.answeredAt = answeredAt;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SubSubject getSubSubject() { return subSubject; }
    public void setSubSubject(SubSubject subSubject) { this.subSubject = subSubject; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
