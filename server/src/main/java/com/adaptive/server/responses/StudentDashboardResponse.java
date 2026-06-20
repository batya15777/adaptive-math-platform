package com.adaptive.server.responses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated, frontend-ready payload for the Student Learning Dashboard.
 * The backend turns raw attempt rows + the ML cluster into actionable insights,
 * so the React side can render directly without further computation.
 */
public class StudentDashboardResponse {

    private StudentSummary student;
    private OverallStats overall;
    private ClusterInfo cluster;
    private List<TopicProgress> topics;
    private List<Recommendation> recommendations;
    private List<Badge> badges;

    public StudentDashboardResponse() {
    }

    public StudentSummary getStudent() { return student; }
    public void setStudent(StudentSummary student) { this.student = student; }

    public OverallStats getOverall() { return overall; }
    public void setOverall(OverallStats overall) { this.overall = overall; }

    public ClusterInfo getCluster() { return cluster; }
    public void setCluster(ClusterInfo cluster) { this.cluster = cluster; }

    public List<TopicProgress> getTopics() { return topics; }
    public void setTopics(List<TopicProgress> topics) { this.topics = topics; }

    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }

    public List<Badge> getBadges() { return badges; }
    public void setBadges(List<Badge> badges) { this.badges = badges; }

    // ── nested DTOs ──────────────────────────────────────────────────────────

    public static class StudentSummary {
        private Long id;
        private String name;
        private int totalStars;

        public StudentSummary() {}
        public StudentSummary(Long id, String name, int totalStars) {
            this.id = id; this.name = name; this.totalStars = totalStars;
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public int getTotalStars() { return totalStars; }
    }

    public static class OverallStats {
        private long totalAttempts;
        private long totalCorrect;
        private int successRate;   // 0–100
        private int masteryLevel;  // highest level reached across sub-subjects

        public OverallStats() {}
        public OverallStats(long totalAttempts, long totalCorrect, int successRate, int masteryLevel) {
            this.totalAttempts = totalAttempts; this.totalCorrect = totalCorrect;
            this.successRate = successRate; this.masteryLevel = masteryLevel;
        }
        public long getTotalAttempts() { return totalAttempts; }
        public long getTotalCorrect() { return totalCorrect; }
        public int getSuccessRate() { return successRate; }
        public int getMasteryLevel() { return masteryLevel; }
    }

    public static class ClusterInfo {
        private boolean assigned;
        private Integer clusterId;
        private String label;
        private List<String> topErrorPatterns;
        private Double avgAccuracy;       // 0–1 (cohort average)
        private LocalDateTime assignedAt;

        public ClusterInfo() {}
        public static ClusterInfo unassigned() {
            ClusterInfo c = new ClusterInfo();
            c.assigned = false;
            c.topErrorPatterns = List.of();
            return c;
        }
        public boolean isAssigned() { return assigned; }
        public void setAssigned(boolean assigned) { this.assigned = assigned; }
        public Integer getClusterId() { return clusterId; }
        public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<String> getTopErrorPatterns() { return topErrorPatterns; }
        public void setTopErrorPatterns(List<String> topErrorPatterns) { this.topErrorPatterns = topErrorPatterns; }
        public Double getAvgAccuracy() { return avgAccuracy; }
        public void setAvgAccuracy(Double avgAccuracy) { this.avgAccuracy = avgAccuracy; }
        public LocalDateTime getAssignedAt() { return assignedAt; }
        public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    }

    public static class TopicProgress {
        private Long subSubjectId;
        private String name;
        private long attempts;
        private long correct;
        private int successRate;   // 0–100
        private int currentLevel;

        public TopicProgress() {}
        public TopicProgress(Long subSubjectId, String name, long attempts, long correct,
                             int successRate, int currentLevel) {
            this.subSubjectId = subSubjectId; this.name = name; this.attempts = attempts;
            this.correct = correct; this.successRate = successRate; this.currentLevel = currentLevel;
        }
        public Long getSubSubjectId() { return subSubjectId; }
        public String getName() { return name; }
        public long getAttempts() { return attempts; }
        public long getCorrect() { return correct; }
        public int getSuccessRate() { return successRate; }
        public int getCurrentLevel() { return currentLevel; }
    }

    public static class Recommendation {
        private String title;
        private String message;
        private String errorPattern;
        private Long recommendedSubSubjectId;   // deep-link target, or null for generic practice
        private String recommendedSubSubjectName;
        private String severity;                // "high" | "medium" | "low"

        public Recommendation() {}
        public Recommendation(String title, String message, String errorPattern,
                              Long recommendedSubSubjectId, String recommendedSubSubjectName, String severity) {
            this.title = title; this.message = message; this.errorPattern = errorPattern;
            this.recommendedSubSubjectId = recommendedSubSubjectId;
            this.recommendedSubSubjectName = recommendedSubSubjectName;
            this.severity = severity;
        }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getErrorPattern() { return errorPattern; }
        public Long getRecommendedSubSubjectId() { return recommendedSubSubjectId; }
        public String getRecommendedSubSubjectName() { return recommendedSubSubjectName; }
        public String getSeverity() { return severity; }
    }

    public static class Badge {
        private String id;
        private String name;
        private String icon;
        private String description;
        private boolean earned;

        public Badge() {}
        public Badge(String id, String name, String icon, String description, boolean earned) {
            this.id = id; this.name = name; this.icon = icon;
            this.description = description; this.earned = earned;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public boolean isEarned() { return earned; }
    }
}
