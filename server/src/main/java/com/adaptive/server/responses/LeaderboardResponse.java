package com.adaptive.server.responses;

import com.adaptive.server.DTOs.LeaderboardEntryDto;
import java.util.List;

public class LeaderboardResponse extends BasicResponse {
    private List<LeaderboardEntryDto> leaderboard;

    public LeaderboardResponse() {
        super();
    }

    public LeaderboardResponse(boolean success, String message, List<LeaderboardEntryDto> leaderboard) {
        super(success, message);
        this.leaderboard = leaderboard;
    }

    public List<LeaderboardEntryDto> getLeaderboard() {
        return leaderboard;
    }

    public void setLeaderboard(List<LeaderboardEntryDto> leaderboard) {
        this.leaderboard = leaderboard;
    }
}
