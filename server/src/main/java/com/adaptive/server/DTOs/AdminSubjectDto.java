package com.adaptive.server.DTOs;

public class AdminSubjectDto {
    private final Long id;
    private final Long topicId;
    private final String name;
    private final long subSubjectCount;
    private final boolean active;
    private final boolean system; // protected (e.g. "Calculation") — rename/disable blocked

    public AdminSubjectDto(Long id, Long topicId, String name, long subSubjectCount,
                           boolean active, boolean system) {
        this.id = id;
        this.topicId = topicId;
        this.name = name;
        this.subSubjectCount = subSubjectCount;
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

    public boolean isActive() {
        return active;
    }

    public boolean isSystem() {
        return system;
    }
}
