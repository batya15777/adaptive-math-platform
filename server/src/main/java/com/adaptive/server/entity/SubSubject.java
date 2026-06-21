package com.adaptive.server.entity;

import javax.persistence.*;

@Entity
@Table(name = "sub_subjects")
public class SubSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Establishing the relation to the Subject table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // Soft-delete / draft flag (mirrors Topic and Subject). DB default TRUE so
    // existing rows stay visible to students under ddl-auto=update.
    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean active = true;

    public SubSubject() {
    }

    public SubSubject(String name, Subject subject) {
        this.name = name;
        this.subject = subject;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Boolean getActive() {
        return active == null ? Boolean.TRUE : active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}