package com.adaptive.server.repository;

import java.time.LocalDateTime;

/**
 * One aggregated cluster group for the read-only Admin ML Groups screen.
 * Produced by StudentClusterAssignmentRepository.findClusterGroups() (GROUP BY clusterId).
 */
public interface AdminClusterGroupProjection {
    Integer getClusterId();
    String getLabel();
    String getTopErrors();         // CSV; parsed to a list in the service
    Double getAvgAccuracy();       // 0..1 (nullable)
    Integer getModelK();
    Long getMemberCount();
    LocalDateTime getLastAssignedAt();
}
