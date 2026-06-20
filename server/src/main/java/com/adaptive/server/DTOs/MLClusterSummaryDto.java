package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Descriptive profile of one cluster. Not strictly needed for the question
 * generator, but persisted as a label and useful for the future admin dashboard.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLClusterSummaryDto {

    @JsonProperty("cluster_id")
    private Integer clusterId;

    private Integer size;

    private String label;

    @JsonProperty("avg_accuracy")
    private Double avgAccuracy;

    @JsonProperty("avg_difficulty")
    private Double avgDifficulty;

    @JsonProperty("top_error_patterns")
    private List<String> topErrorPatterns;

    public MLClusterSummaryDto() {
    }

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Double getAvgAccuracy() { return avgAccuracy; }
    public void setAvgAccuracy(Double avgAccuracy) { this.avgAccuracy = avgAccuracy; }

    public Double getAvgDifficulty() { return avgDifficulty; }
    public void setAvgDifficulty(Double avgDifficulty) { this.avgDifficulty = avgDifficulty; }

    public List<String> getTopErrorPatterns() { return topErrorPatterns; }
    public void setTopErrorPatterns(List<String> topErrorPatterns) { this.topErrorPatterns = topErrorPatterns; }
}
