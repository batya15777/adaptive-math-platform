package com.adaptive.server.service;

import com.adaptive.server.DTOs.*;
import com.adaptive.server.entity.StudentClusterAssignment;
import com.adaptive.server.repository.AttemptFeatureProjection;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.StudentClusterAssignmentRepository;
import com.adaptive.server.responses.MLClusteringResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Orchestrates unsupervised clustering of students:
 *   1. pulls every exercise attempt as a lightweight ML projection,
 *   2. groups attempts per student into a JSON payload,
 *   3. POSTs them to the Python K-Means microservice,
 *   4. upserts the returned user_id -> cluster_id mapping so the question
 *      generator can use it later.
 *
 * The dedicated {@link RestTemplate} has explicit connect/read timeouts so a
 * down or slow microservice fails fast with a clear message instead of hanging.
 */
@Service
public class MLClusteringService {

    private static final Logger log = LoggerFactory.getLogger(MLClusteringService.class);
    private static final String RUN_ENDPOINT = "/clustering/run";
    // Only cluster users whose current role is STUDENT.
    private static final String STUDENT_ROLE = "STUDENT";

    private final RestTemplate restTemplate;
    private final ExerciseAttemptRepository attemptRepository;
    private final StudentClusterAssignmentRepository assignmentRepository;

    @Value("${ml.clustering.url:http://localhost:8002}")
    private String clusteringUrl;

    public MLClusteringService(RestTemplateBuilder restTemplateBuilder,
                               ExerciseAttemptRepository attemptRepository,
                               StudentClusterAssignmentRepository assignmentRepository) {
        // Connect quickly; allow generous read time since fitting K-Means on many
        // students can take a few seconds.
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
        this.attemptRepository = attemptRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Runs a full clustering pass over all students with attempt history.
     *
     * @param requestedK optional fixed number of clusters; null lets the
     *                   microservice auto-select k via silhouette score.
     */
    @Transactional
    public MLClusteringResponse runClustering(Integer requestedK) {
        List<MLStudentFeaturesDto> students = buildStudentFeatures();
        if (students.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No exercise attempts available to cluster yet.");
        }

        MLClusteringRequest request = new MLClusteringRequest(students, requestedK);
        MLClusteringResponse response = callMicroservice(request);
        persistAssignments(response);

        log.info("Clustering complete: {} students grouped into k={} clusters (silhouette={})",
                students.size(), response.getK(), response.getSilhouetteScore());
        return response;
    }

    /** Reads a student's most recent cluster assignment (for the question generator). */
    @Transactional(readOnly = true)
    public Optional<StudentClusterAssignment> getAssignmentForUser(Long userId) {
        return assignmentRepository.findByUserId(userId);
    }

    // ── internals ──────────────────────────────────────────────────────────

    /** Pulls attempts (current students only) and groups them per student into the request DTO. */
    private List<MLStudentFeaturesDto> buildStudentFeatures() {
        // Only cluster users whose CURRENT role is STUDENT — ex-students who became
        // admins must not be pulled into cohorts via their old attempts.
        List<AttemptFeatureProjection> rows = attemptRepository.findAllAttemptFeaturesForRole(STUDENT_ROLE);

        // LinkedHashMap keeps a stable, deterministic student order in the payload.
        Map<Long, List<MLAttemptFeatureDto>> attemptsByUser = new LinkedHashMap<>();
        for (AttemptFeatureProjection row : rows) {
            if (row.getUserId() == null) {
                continue;
            }
            attemptsByUser
                    .computeIfAbsent(row.getUserId(), id -> new ArrayList<>())
                    .add(new MLAttemptFeatureDto(
                            row.getErrorPattern(),
                            row.getQuestionType(),
                            row.getDifficultyLevel(),
                            row.getIsCorrect(),
                            row.getUserAnswer(),
                            row.getAnsweredAt()));
        }

        List<MLStudentFeaturesDto> students = new ArrayList<>(attemptsByUser.size());
        for (Map.Entry<Long, List<MLAttemptFeatureDto>> entry : attemptsByUser.entrySet()) {
            students.add(new MLStudentFeaturesDto(entry.getKey(), entry.getValue()));
        }
        return students;
    }

    /** Calls the Python service, converting transport failures into clear HTTP errors. */
    private MLClusteringResponse callMicroservice(MLClusteringRequest request) {
        String url = clusteringUrl + RUN_ENDPOINT;
        try {
            MLClusteringResponse response =
                    restTemplate.postForObject(url, request, MLClusteringResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Clustering microservice returned an empty response.");
            }
            return response;
        } catch (ResourceAccessException e) {
            // Connection refused / timeout — service is down or unreachable.
            log.error("Clustering microservice unreachable at {}: {}", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Clustering microservice is unavailable at " + clusteringUrl +
                    ". Start it with: uv run uvicorn main:app --port 8001", e);
        } catch (RestClientException e) {
            // Non-2xx response or body mapping failure.
            log.error("Clustering microservice call failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Clustering microservice call failed: " + e.getMessage(), e);
        }
    }

    /** Upserts one row per user with their assigned cluster id and label. */
    private void persistAssignments(MLClusteringResponse response) {
        if (response.getAssignments() == null || response.getAssignments().isEmpty()) {
            log.warn("Clustering response contained no assignments to persist.");
            return;
        }

        // Index the cluster summaries by id so each student row can carry its cohort's
        // label, dominant errors and average accuracy (used by the dashboard + generators).
        Map<Integer, MLClusterSummaryDto> summaryByCluster = new HashMap<>();
        if (response.getClusters() != null) {
            for (MLClusterSummaryDto cluster : response.getClusters()) {
                summaryByCluster.put(cluster.getClusterId(), cluster);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (MLClusterAssignmentDto assignment : response.getAssignments()) {
            if (assignment.getUserId() == null || assignment.getClusterId() == null) {
                continue;
            }
            StudentClusterAssignment entity = assignmentRepository
                    .findByUserId(assignment.getUserId())
                    .orElseGet(() -> new StudentClusterAssignment(assignment.getUserId()));

            MLClusterSummaryDto summary = summaryByCluster.get(assignment.getClusterId());
            entity.setClusterId(assignment.getClusterId());
            entity.setModelK(response.getK());
            entity.setAssignedAt(now);
            if (summary != null) {
                entity.setClusterLabel(summary.getLabel());
                entity.setClusterAvgAccuracy(summary.getAvgAccuracy());
                entity.setClusterTopErrors(summary.getTopErrorPatterns() == null ? null
                        : String.join(",", summary.getTopErrorPatterns()));
            }
            assignmentRepository.save(entity);
        }
    }
}
