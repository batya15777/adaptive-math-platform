package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload POSTed to the Python clustering microservice (/clustering/run).
 * {@code nClusters} is optional — when null, the microservice auto-selects k.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MLClusteringRequest {

    private List<MLStudentFeaturesDto> students;

    @JsonProperty("n_clusters")
    private Integer nClusters;

    public MLClusteringRequest() {
    }

    public MLClusteringRequest(List<MLStudentFeaturesDto> students, Integer nClusters) {
        this.students = students;
        this.nClusters = nClusters;
    }

    public List<MLStudentFeaturesDto> getStudents() { return students; }
    public void setStudents(List<MLStudentFeaturesDto> students) { this.students = students; }

    public Integer getNClusters() { return nClusters; }
    public void setNClusters(Integer nClusters) { this.nClusters = nClusters; }
}
