package com.adaptive.server.DTOs;

public class AdminTopicDto {
    private final Long id;
    private final String name;
    private final long subjectCount;
    private final boolean active;

    public AdminTopicDto(Long id, String name, long subjectCount, boolean active) {
        this.id = id;
        this.name = name;
        this.subjectCount = subjectCount;
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

    public boolean isActive() {
        return active;
    }
}
