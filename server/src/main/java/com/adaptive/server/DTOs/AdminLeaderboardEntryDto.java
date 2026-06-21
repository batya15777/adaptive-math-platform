package com.adaptive.server.DTOs;

public class AdminLeaderboardEntryDto {
    private final Long id;
    private final String name;
    private final int totalStars;

    public AdminLeaderboardEntryDto(Long id, String name, int totalStars) {
        this.id = id;
        this.name = name;
        this.totalStars = totalStars;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getTotalStars() { return totalStars; }
}
