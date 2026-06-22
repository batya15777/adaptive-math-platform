package com.adaptive.server.DTOs;

public class AdminTopicDto {
    private final Long id;
    private final String name;
    private final long subjectCount;        // total subjects (active + draft), for display
    private final long activeSubjectCount;  // active subjects only, drives the publish guard/UI
    private final boolean active;

    public AdminTopicDto(Long id, String name, long subjectCount, long activeSubjectCount, boolean active) {
        this.id = id;
        this.name = name;
        this.subjectCount = subjectCount;
        this.activeSubjectCount = activeSubjectCount;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSubjectCount() {
        return subjectCount;
    }

    public long getActiveSubjectCount() {
        return activeSubjectCount;
    }

    public boolean isActive() {
        return active;
    }
}
