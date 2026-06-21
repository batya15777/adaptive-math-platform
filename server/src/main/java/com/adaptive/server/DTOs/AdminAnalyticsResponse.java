package com.adaptive.server.DTOs;

import java.util.List;

public class AdminAnalyticsResponse {
    private final AdminAnalyticsSummaryDto summary;
    private final List<AdminSubSubjectStatDto> hardestSubSubjects;
    private final List<AdminErrorStatDto> commonErrors;
    private final List<AdminLeaderboardEntryDto> leaderboard;

    public AdminAnalyticsResponse(AdminAnalyticsSummaryDto summary,
                                  List<AdminSubSubjectStatDto> hardestSubSubjects,
                                  List<AdminErrorStatDto> commonErrors,
                                  List<AdminLeaderboardEntryDto> leaderboard) {
        this.summary = summary;
        this.hardestSubSubjects = hardestSubSubjects;
        this.commonErrors = commonErrors;
        this.leaderboard = leaderboard;
    }

    public AdminAnalyticsSummaryDto getSummary() { return summary; }
    public List<AdminSubSubjectStatDto> getHardestSubSubjects() { return hardestSubSubjects; }
    public List<AdminErrorStatDto> getCommonErrors() { return commonErrors; }
    public List<AdminLeaderboardEntryDto> getLeaderboard() { return leaderboard; }
}
