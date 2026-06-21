package com.adaptive.server.DTOs;

import java.time.LocalDateTime;
import java.util.List;

public class AdminMLGroupDto {
    private final Integer clusterId;
    private final String label;
    private final long memberCount;
    private final Double avgAccuracy;        // 0..1, nullable
    private final List<String> topErrors;    // never null (empty list if none)
    private final Integer modelK;
    private final LocalDateTime lastAssignedAt;  // nullable

    public AdminMLGroupDto(Integer clusterId, String label, long memberCount, Double avgAccuracy,
                           List<String> topErrors, Integer modelK, LocalDateTime lastAssignedAt) {
        this.clusterId = clusterId;
        this.label = label;
        this.memberCount = memberCount;
        this.avgAccuracy = avgAccuracy;
        this.topErrors = topErrors;
        this.modelK = modelK;
        this.lastAssignedAt = lastAssignedAt;
    }

    public Integer getClusterId() { return clusterId; }
    public String getLabel() { return label; }
    public long getMemberCount() { return memberCount; }
    public Double getAvgAccuracy() { return avgAccuracy; }
    public List<String> getTopErrors() { return topErrors; }
    public Integer getModelK() { return modelK; }
    public LocalDateTime getLastAssignedAt() { return lastAssignedAt; }
}
