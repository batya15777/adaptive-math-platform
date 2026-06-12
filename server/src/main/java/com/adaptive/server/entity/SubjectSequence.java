package com.adaptive.server.entity;

import javax.persistence.*;

@Entity
@Table(name = "subject_sequence")
public class SubjectSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_id", nullable = false)
    private Subject currentSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_id", nullable = false)
    private Subject nextSubject;

    public SubjectSequence() {
    }

    public SubjectSequence(Subject currentSubject, Subject nextSubject) {
        this.currentSubject = currentSubject;
        this.nextSubject = nextSubject;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subject getCurrentSubject() {
        return currentSubject;
    }

    public void setCurrentSubject(Subject currentSubject) {
        this.currentSubject = currentSubject;
    }

    public Subject getNextSubject() {
        return nextSubject;
    }

    public void setNextSubject(Subject nextSubject) {
        this.nextSubject = nextSubject;
    }
}
