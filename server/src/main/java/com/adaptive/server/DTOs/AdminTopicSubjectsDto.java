package com.adaptive.server.DTOs;

import java.util.List;

/**
 * Response for GET /admin/content/topics/{topicId}/subjects.
 * Carries the parent topic info so the admin screen has a correct title even on
 * a direct visit / refresh, plus the active-subject count for UI guards.
 */
public class AdminTopicSubjectsDto {
    private final Long topicId;
    private final String topicName;
    private final boolean topicActive;
    private final long activeSubjectCount;
    private final List<AdminSubjectDto> subjects;

    public AdminTopicSubjectsDto(Long topicId, String topicName, boolean topicActive,
                                 long activeSubjectCount, List<AdminSubjectDto> subjects) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.topicActive = topicActive;
        this.activeSubjectCount = activeSubjectCount;
        this.subjects = subjects;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public boolean isTopicActive() {
        return topicActive;
    }

    public long getActiveSubjectCount() {
        return activeSubjectCount;
    }

    public List<AdminSubjectDto> getSubjects() {
        return subjects;
    }
}
