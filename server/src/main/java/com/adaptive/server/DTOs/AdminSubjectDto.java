package com.adaptive.server.DTOs;

public class AdminSubjectDto {
    private final Long id;
    private final Long topicId;
    private final String name;
    private final long subSubjectCount;        // total sub-subjects (active + draft), for display
    private final long activeSubSubjectCount;  // active only — drives the publish guard/UI
    private final boolean active;
    private final boolean system; // protected (e.g. "Calculation") — rename/disable blocked

    public AdminSubjectDto(Long id, Long topicId, String name, long subSubjectCount,
                           long activeSubSubjectCount, boolean active, boolean system) {
        this.id = id;
        this.topicId = topicId;
        this.name = name;
        this.subSubjectCount = subSubjectCount;
        this.activeSubSubjectCount = activeSubSubjectCount;
        this.active = active;
        this.system = system;
    }

    public Long getId() {
        return id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getName() {
        return name;
    }

    public long getSubSubjectCount() {
        return subSubjectCount;
    }

    public long getActiveSubSubjectCount() {
        return activeSubSubjectCount;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSystem() {
        return system;
    }
}
