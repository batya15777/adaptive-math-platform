package com.adaptive.server.entity;

import javax.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    private String name;

    // Soft-delete / draft flag (mirrors Topic). DB default TRUE so existing rows
    // stay visible to students after the column is added under ddl-auto=update.
    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean active = true;

    public Subject() {
    }

    public Subject(String name) {
        this.name = name;
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

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Boolean getActive() {
        return active == null ? Boolean.TRUE : active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
