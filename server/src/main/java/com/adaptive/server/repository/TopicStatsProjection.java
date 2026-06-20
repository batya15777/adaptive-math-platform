package com.adaptive.server.repository;

/**
 * Per-sub-subject attempt stats for one student, aggregated in a single GROUP BY query.
 * Powers the dashboard's "progress by topic" / success-rate visualisations.
 */
public interface TopicStatsProjection {
    Long getSubSubjectId();
    String getSubSubjectName();
    Long getTotal();
    Long getCorrect();
}
