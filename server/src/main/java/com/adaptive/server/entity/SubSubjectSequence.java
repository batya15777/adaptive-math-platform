package com.adaptive.server.entity;

import javax.persistence.*;

@Entity
@Table(name = "subsubject_sequence")
public class SubSubjectSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_id", nullable = false)
    private SubSubject currentSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_id", nullable = false)
    private SubSubject nextSubject;

    public SubSubjectSequence() {
    }

    public SubSubjectSequence(SubSubject currentSubject, SubSubject nextSubject) {
        this.currentSubject = currentSubject;
        this.nextSubject = nextSubject;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SubSubject getCurrentSubject() {
        return currentSubject;
    }

    public void setCurrentSubject(SubSubject currentSubject) {
        this.currentSubject = currentSubject;
    }

    public SubSubject getNextSubject() {
        return nextSubject;
    }

    public void setNextSubject(SubSubject nextSubject) {
        this.nextSubject = nextSubject;
    }
}

