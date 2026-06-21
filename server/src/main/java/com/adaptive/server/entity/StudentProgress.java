package com.adaptive.server.entity;

import javax.persistence.*;

@Entity
@Table(name = "student_progress")
public class StudentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_subject_id", nullable = false)
    private SubSubject subSubject;

    @Column(name = "current_level")
    private Integer currentLevel;

    @Column(name = "current_progress")
    private Integer currentProgress;

    @Column(name = "is_active")
    private Boolean isActive;

    // Temporary easier-practice mode entered after repeated fails (level frozen meanwhile).
    @Column(name = "in_sub_level")
    private Boolean inSubLevel = false;

    // FAILED questions in a row at the normal level — toward entering the sub-level.
    @Column(name = "consecutive_fails")
    private Integer consecutiveFails = 0;

    // SOLVED questions inside the sub-level — toward returning to the normal level.
    @Column(name = "sub_level_progress")
    private Integer subLevelProgress = 0;

    // Total attempt count marking the start of the current level-up window (refill point).
    @Column(name = "level_up_baseline")
    private Integer levelUpBaseline = 0;

    // Consecutive correct answers in a row (resets to 0 on any wrong answer).
    @Column(name = "current_streak")
    private Integer currentStreak = 0;

    // The question currently presented to this student for this sub-subject.
    // Null once the question is resolved (SOLVED or FAILED). Used to resume
    // the same question on page reload instead of generating a new one.
    @Column(name = "active_question_id")
    private Long activeQuestionId;

    public StudentProgress() {
    }

    public StudentProgress(User user, SubSubject subSubject, Integer currentLevel, Integer currentProgress, Boolean isActive) {
        this.user = user;
        this.subSubject = subSubject;
        this.currentLevel = currentLevel;
        this.currentProgress = currentProgress;
        this.isActive = isActive;
    }

    public Integer getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        this.currentProgress = currentProgress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SubSubject getSubSubject() {
        return subSubject;
    }

    public void setSubSubject(SubSubject subSubject) {
        this.subSubject = subSubject;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getInSubLevel() {
        return inSubLevel != null && inSubLevel;
    }

    public void setInSubLevel(Boolean inSubLevel) {
        this.inSubLevel = inSubLevel;
    }

    public Integer getConsecutiveFails() {
        return consecutiveFails == null ? 0 : consecutiveFails;
    }

    public void setConsecutiveFails(Integer consecutiveFails) {
        this.consecutiveFails = consecutiveFails;
    }

    public Integer getSubLevelProgress() {
        return subLevelProgress == null ? 0 : subLevelProgress;
    }

    public void setSubLevelProgress(Integer subLevelProgress) {
        this.subLevelProgress = subLevelProgress;
    }

    public Integer getLevelUpBaseline() {
        return levelUpBaseline == null ? 0 : levelUpBaseline;
    }

    public void setLevelUpBaseline(Integer levelUpBaseline) {
        this.levelUpBaseline = levelUpBaseline;
    }

    public Integer getCurrentStreak() {
        return currentStreak == null ? 0 : currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Long getActiveQuestionId() {
        return activeQuestionId;
    }

    public void setActiveQuestionId(Long activeQuestionId) {
        this.activeQuestionId = activeQuestionId;
    }
}
