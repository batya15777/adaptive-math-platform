package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A single user_id -> cluster_id mapping returned by the microservice. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLClusterAssignmentDto {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("cluster_id")
    private Integer clusterId;

    public MLClusterAssignmentDto() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }
}
