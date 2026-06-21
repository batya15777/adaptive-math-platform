package com.adaptive.server.DTOs;

import java.time.LocalDateTime;
import java.util.List;

public class AdminMLGroupsResponse {
    private final LocalDateTime lastRunAt;   // nullable (no runs yet)
    private final long totalAssigned;
    private final List<AdminMLGroupDto> groups;

    public AdminMLGroupsResponse(LocalDateTime lastRunAt, long totalAssigned, List<AdminMLGroupDto> groups) {
        this.lastRunAt = lastRunAt;
        this.totalAssigned = totalAssigned;
        this.groups = groups;
    }

    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public long getTotalAssigned() { return totalAssigned; }
    public List<AdminMLGroupDto> getGroups() { return groups; }
}
