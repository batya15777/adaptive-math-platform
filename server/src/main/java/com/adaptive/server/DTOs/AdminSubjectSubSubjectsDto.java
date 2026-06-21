package com.adaptive.server.DTOs;

import java.util.List;

/**
 * Response for GET /admin/content/subjects/{subjectId}/sub-subjects.
 * Carries the parent subject (and topic) info so the admin screen has a correct
 * title/breadcrumb on a direct visit / refresh, plus the active-sub-subject count
 * for UI guards and the {@code subjectSystem} flag (parent subject is protected,
 * so new sub-subjects cannot be added).
 */
public class AdminSubjectSubSubjectsDto {
    private final Long subjectId;
    private final String subjectName;
    private final boolean subjectActive;
    private final boolean subjectSystem;
    private final Long topicId;
    private final String topicName;
    private final long activeSubSubjectCount;
    private final List<AdminSubSubjectDto> subSubjects;

    public AdminSubjectSubSubjectsDto(Long subjectId, String subjectName, boolean subjectActive,
                                      boolean subjectSystem, Long topicId, String topicName,
                                      long activeSubSubjectCount, List<AdminSubSubjectDto> subSubjects) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.subjectActive = subjectActive;
        this.subjectSystem = subjectSystem;
        this.topicId = topicId;
        this.topicName = topicName;
        this.activeSubSubjectCount = activeSubSubjectCount;
        this.subSubjects = subSubjects;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public boolean isSubjectActive() {
        return subjectActive;
    }

    public boolean isSubjectSystem() {
        return subjectSystem;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public long getActiveSubSubjectCount() {
        return activeSubSubjectCount;
    }

    public List<AdminSubSubjectDto> getSubSubjects() {
        return subSubjects;
    }
}
