package com.adaptive.server.responses;

import com.adaptive.server.DTOs.MLClusterAssignmentDto;
import com.adaptive.server.DTOs.MLClusterSummaryDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MLClusteringResponse {

    private Integer k;

    @JsonProperty("silhouette_score")
    private Double silhouetteScore;

    private List<MLClusterAssignmentDto> assignments;

    private List<MLClusterSummaryDto> clusters;

    public MLClusteringResponse() {
    }

    public Integer getK() { return k; }
    public void setK(Integer k) { this.k = k; }

    public Double getSilhouetteScore() { return silhouetteScore; }
    public void setSilhouetteScore(Double silhouetteScore) { this.silhouetteScore = silhouetteScore; }

    public List<MLClusterAssignmentDto> getAssignments() { return assignments; }
    public void setAssignments(List<MLClusterAssignmentDto> assignments) { this.assignments = assignments; }

    public List<MLClusterSummaryDto> getClusters() { return clusters; }
    public void setClusters(List<MLClusterSummaryDto> clusters) { this.clusters = clusters; }
}
