package com.adaptive.server.DTOs;

/**
 * Aggregated attempt stats for a single sub-subject (learning area) across all students.
 * Named "SubSubject" (not "Topic") because the underlying GROUP BY is per sub-subject.
 */
public class AdminSubSubjectStatDto {
    private final Long subSubjectId;
    private final String name;
    private final long attempts;
    private final long correct;
    private final int successRate;  // 0..100

    public AdminSubSubjectStatDto(Long subSubjectId, String name, long attempts, long correct, int successRate) {
        this.subSubjectId = subSubjectId;
        this.name = name;
        this.attempts = attempts;
        this.correct = correct;
        this.successRate = successRate;
    }

    public Long getSubSubjectId() { return subSubjectId; }
    public String getName() { return name; }
    public long getAttempts() { return attempts; }
    public long getCorrect() { return correct; }
    public int getSuccessRate() { return successRate; }
}
