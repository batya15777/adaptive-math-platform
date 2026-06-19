package com.adaptive.server.entity;

import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "tutor_chat_messages")
public class TutorChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_subject_id")
    private SubSubject subSubject;

    @Column(name = "student_message", nullable = false, columnDefinition = "TEXT")
    private String studentMessage;

    @Column(name = "tutor_response", nullable = false, columnDefinition = "TEXT")
    private String tutorResponse;

    @Column(name = "guidance_level")
    private Integer guidanceLevel;

    private String action;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TutorChatMessage() {
    }

    public TutorChatMessage(User user, Question question, SubSubject subSubject,
                            String studentMessage, String tutorResponse,
                            Integer guidanceLevel, String action, LocalDateTime createdAt) {
        this.user = user;
        this.question = question;
        this.subSubject = subSubject;
        this.studentMessage = studentMessage;
        this.tutorResponse = tutorResponse;
        this.guidanceLevel = guidanceLevel;
        this.action = action;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Question getQuestion() {
        return question;
    }

    public SubSubject getSubSubject() {
        return subSubject;
    }

    public String getStudentMessage() {
        return studentMessage;
    }

    public String getTutorResponse() {
        return tutorResponse;
    }

    public Integer getGuidanceLevel() {
        return guidanceLevel;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setSubSubject(SubSubject subSubject) {
        this.subSubject = subSubject;
    }

    public void setStudentMessage(String studentMessage) {
        this.studentMessage = studentMessage;
    }

    public void setTutorResponse(String tutorResponse) {
        this.tutorResponse = tutorResponse;
    }

    public void setGuidanceLevel(Integer guidanceLevel) {
        this.guidanceLevel = guidanceLevel;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
