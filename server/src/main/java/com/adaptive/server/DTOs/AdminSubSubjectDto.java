package com.adaptive.server.DTOs;

public class AdminSubSubjectDto {
    private final Long id;
    private final Long subjectId;
    private final String name;
    private final long templateCount;
    private final boolean active;
    private final boolean system; // protected (parent subject is protected) — rename/disable blocked

    public AdminSubSubjectDto(Long id, Long subjectId, String name, long templateCount,
                             boolean active, boolean system) {
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
        this.templateCount = templateCount;
        this.active = active;
        this.system = system;
    }

    public Long getId() {
        return id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getName() {
        return name;
    }

    public long getTemplateCount() {
        return templateCount;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSystem() {
        return system;
    }
}
