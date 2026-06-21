package com.adaptive.server.DTOs;

public class AdminAnalyticsSummaryDto {
    private final long totalUsers;
    private final long totalStudents;
    private final long totalAttempts;
    private final int successRate;        // 0..100
    private final long activeLearners7d;

    public AdminAnalyticsSummaryDto(long totalUsers, long totalStudents, long totalAttempts,
                                    int successRate, long activeLearners7d) {
        this.totalUsers = totalUsers;
        this.totalStudents = totalStudents;
        this.totalAttempts = totalAttempts;
        this.successRate = successRate;
        this.activeLearners7d = activeLearners7d;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getTotalStudents() { return totalStudents; }
    public long getTotalAttempts() { return totalAttempts; }
    public int getSuccessRate() { return successRate; }
    public long getActiveLearners7d() { return activeLearners7d; }
}
