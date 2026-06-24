package com.adaptive.server.service;

import com.adaptive.server.DTOs.MLClusterAssignmentDto;
import com.adaptive.server.DTOs.MLClusterSummaryDto;
import com.adaptive.server.DTOs.MLClusteringRequest;
import com.adaptive.server.DTOs.MLStudentFeaturesDto;
import com.adaptive.server.entity.StudentClusterAssignment;
import com.adaptive.server.repository.AttemptFeatureProjection;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.StudentClusterAssignmentRepository;
import com.adaptive.server.responses.MLClusteringResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MLClusteringService} — the orchestrator that pulls student
 * attempts, POSTs them to the Python K-Means microservice, and upserts the returned
 * cluster assignments.
 *
 * <p>Style mirrors {@code LevelManagerServiceTest}: JUnit 5, plain Mockito {@code mock()}
 * in {@code @BeforeEach}, {@code @Nested} groups and {@code ArgumentCaptor}. The service
 * builds its own {@link RestTemplate} from a {@link RestTemplateBuilder} inside the
 * constructor, so after construction we swap in a mock {@code restTemplate} (and a
 * deterministic {@code clusteringUrl}) via {@link ReflectionTestUtils} — no need to mock
 * the builder's fluent chain.
 */
class MLClusteringServiceTest {

    private RestTemplate restTemplate;
    private ExerciseAttemptRepository attemptRepository;
    private StudentClusterAssignmentRepository assignmentRepository;

    private MLClusteringService service;

    private static final String BASE_URL = "http://test-clustering";
    private static final String RUN_URL = BASE_URL + "/clustering/run";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        attemptRepository = mock(ExerciseAttemptRepository.class);
        assignmentRepository = mock(StudentClusterAssignmentRepository.class);

        // Construct with a real builder, then replace the built RestTemplate + the
        // @Value-injected URL (both null/real otherwise in a non-Spring unit test).
        service = new MLClusteringService(new RestTemplateBuilder(), attemptRepository, assignmentRepository);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "clusteringUrl", BASE_URL);
    }

    // ── test helpers ─────────────────────────────────────────────────────────

    private AttemptFeatureProjection projection(Long userId, String errorPattern, String questionType,
                                                Integer difficulty, Boolean correct, String answer,
                                                LocalDateTime at) {
        AttemptFeatureProjection p = mock(AttemptFeatureProjection.class);
        when(p.getUserId()).thenReturn(userId);
        when(p.getErrorPattern()).thenReturn(errorPattern);
        when(p.getQuestionType()).thenReturn(questionType);
        when(p.getDifficultyLevel()).thenReturn(difficulty);
        when(p.getIsCorrect()).thenReturn(correct);
        when(p.getUserAnswer()).thenReturn(answer);
        when(p.getAnsweredAt()).thenReturn(at);
        return p;
    }

    private void stubAttempts(AttemptFeatureProjection... rows) {
        when(attemptRepository.findAllAttemptFeaturesForRole("STUDENT")).thenReturn(List.of(rows));
    }

    /** A single, valid student so {@code runClustering} passes the "no attempts" guard. */
    private void stubOneStudent() {
        stubAttempts(projection(1L, "ERR", "ADD", 2, false, "x", NOW));
    }

    private void stubMicroserviceReturns(MLClusteringResponse response) {
        when(restTemplate.postForObject(eq(RUN_URL), any(), eq(MLClusteringResponse.class)))
                .thenReturn(response);
    }

    private MLClusterAssignmentDto assignment(Long userId, Integer clusterId) {
        MLClusterAssignmentDto a = new MLClusterAssignmentDto();
        a.setUserId(userId);
        a.setClusterId(clusterId);
        return a;
    }

    private MLClusterSummaryDto summary(Integer clusterId, String label, Double avgAccuracy,
                                        List<String> topErrors) {
        MLClusterSummaryDto s = new MLClusterSummaryDto();
        s.setClusterId(clusterId);
        s.setLabel(label);
        s.setAvgAccuracy(avgAccuracy);
        s.setTopErrorPatterns(topErrors);
        return s;
    }

    private MLClusteringResponse response(Integer k, List<MLClusterAssignmentDto> assignments,
                                          List<MLClusterSummaryDto> clusters) {
        MLClusteringResponse r = new MLClusteringResponse();
        r.setK(k);
        r.setSilhouetteScore(0.5);
        r.setAssignments(assignments);
        r.setClusters(clusters);
        return r;
    }

    private MLClusteringRequest captureRequest() {
        ArgumentCaptor<MLClusteringRequest> captor = ArgumentCaptor.forClass(MLClusteringRequest.class);
        verify(restTemplate).postForObject(eq(RUN_URL), captor.capture(), eq(MLClusteringResponse.class));
        return captor.getValue();
    }

    private StudentClusterAssignment captureSaved() {
        ArgumentCaptor<StudentClusterAssignment> captor = ArgumentCaptor.forClass(StudentClusterAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        return captor.getValue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Service instantiates without exceptions")
    void serviceInstantiates() {
        assertNotNull(service);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("runClustering() — orchestration & guards")
    class RunClustering {

        @Test
        @DisplayName("No attempts → 422 and the microservice is never called, nothing persisted")
        void noAttempts_throws422_andDoesNotCallMicroservice() {
            when(attemptRepository.findAllAttemptFeaturesForRole("STUDENT"))
                    .thenReturn(Collections.emptyList());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.runClustering(null));

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
            verify(restTemplate, never()).postForObject(anyString(), any(), eq(MLClusteringResponse.class));
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Happy path → POSTs to <url>/clustering/run and returns the microservice response")
        void happyPath_postsToCorrectUrl_andReturnsResponse() {
            stubOneStudent();
            MLClusteringResponse resp = response(3,
                    List.of(assignment(1L, 0)),
                    List.of(summary(0, "Strugglers", 0.4, List.of("ERR"))));
            stubMicroserviceReturns(resp);
            when(assignmentRepository.findByUserId(1L)).thenReturn(Optional.empty());

            MLClusteringResponse out = service.runClustering(3);

            assertSame(resp, out);
            verify(restTemplate).postForObject(eq(RUN_URL), any(MLClusteringRequest.class),
                    eq(MLClusteringResponse.class));
        }

        @Test
        @DisplayName("An explicit k is forwarded as n_clusters in the request")
        void explicitK_isForwarded() {
            stubOneStudent();
            stubMicroserviceReturns(response(5, List.of(), List.of()));

            service.runClustering(5);

            assertEquals(Integer.valueOf(5), captureRequest().getNClusters());
        }

        @Test
        @DisplayName("A null k is forwarded as null (microservice auto-selects k)")
        void nullK_isForwarded() {
            stubOneStudent();
            stubMicroserviceReturns(response(2, List.of(), List.of()));

            service.runClustering(null);

            assertNull(captureRequest().getNClusters());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("buildStudentFeatures() — grouping the attempt projection")
    class BuildStudentFeatures {

        @Test
        @DisplayName("Groups multiple attempts of the same user into one student entry")
        void groupsMultipleAttemptsPerUser() {
            stubAttempts(
                    projection(1L, "E1", "ADD", 1, false, "a", NOW),
                    projection(1L, "E2", "SUB", 2, true, "b", NOW));
            stubMicroserviceReturns(response(1, List.of(), List.of()));

            service.runClustering(null);

            List<MLStudentFeaturesDto> students = captureRequest().getStudents();
            assertEquals(1, students.size());
            assertEquals(Long.valueOf(1L), students.get(0).getUserId());
            assertEquals(2, students.get(0).getAttempts().size());
        }

        @Test
        @DisplayName("Rows with a null user id are skipped")
        void skipsNullUserId() {
            stubAttempts(
                    projection(1L, "E1", "ADD", 1, false, "a", NOW),
                    projection(null, "E2", "SUB", 2, true, "b", NOW));
            stubMicroserviceReturns(response(1, List.of(), List.of()));

            service.runClustering(null);

            List<MLStudentFeaturesDto> students = captureRequest().getStudents();
            assertEquals(1, students.size());
            assertEquals(Long.valueOf(1L), students.get(0).getUserId());
        }

        @Test
        @DisplayName("Student order follows first-seen order (deterministic LinkedHashMap)")
        void preservesFirstSeenOrder() {
            stubAttempts(
                    projection(5L, "E", "ADD", 1, false, "a", NOW),
                    projection(2L, "E", "ADD", 1, false, "a", NOW),
                    projection(9L, "E", "ADD", 1, false, "a", NOW),
                    projection(2L, "E", "ADD", 1, true, "b", NOW)); // 2 seen again, must not reorder
            stubMicroserviceReturns(response(1, List.of(), List.of()));

            service.runClustering(null);

            List<MLStudentFeaturesDto> students = captureRequest().getStudents();
            assertEquals(List.of(5L, 2L, 9L),
                    students.stream().map(MLStudentFeaturesDto::getUserId).toList());
        }

        @Test
        @DisplayName("Pulls attempts for the STUDENT role only")
        void pullsStudentRoleOnly() {
            stubOneStudent();
            stubMicroserviceReturns(response(1, List.of(), List.of()));

            service.runClustering(null);

            verify(attemptRepository).findAllAttemptFeaturesForRole("STUDENT");
        }

        @Test
        @DisplayName("Projection columns map onto the attempt DTO fields")
        void mapsAttemptFields() {
            stubAttempts(projection(1L, "CONFUSED_ADD", "ADDITION", 3, true, "42", NOW));
            stubMicroserviceReturns(response(1, List.of(), List.of()));

            service.runClustering(null);

            var attempt = captureRequest().getStudents().get(0).getAttempts().get(0);
            assertEquals("CONFUSED_ADD", attempt.getErrorPattern());
            assertEquals("ADDITION", attempt.getQuestionType());
            assertEquals(Integer.valueOf(3), attempt.getDifficultyLevel());
            assertEquals(Boolean.TRUE, attempt.getIsCorrect());
            assertEquals("42", attempt.getUserAnswer());
            assertEquals(NOW, attempt.getAnsweredAt());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("callMicroservice() — transport error mapping")
    class CallMicroservice {

        @Test
        @DisplayName("Null body → 502 BAD_GATEWAY and nothing is persisted")
        void nullResponse_throws502() {
            stubOneStudent();
            when(restTemplate.postForObject(eq(RUN_URL), any(), eq(MLClusteringResponse.class)))
                    .thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.runClustering(null));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Connection refused / timeout → 503 SERVICE_UNAVAILABLE")
        void resourceAccessException_throws503() {
            stubOneStudent();
            when(restTemplate.postForObject(eq(RUN_URL), any(), eq(MLClusteringResponse.class)))
                    .thenThrow(new ResourceAccessException("connection refused"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.runClustering(null));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Non-2xx / body mapping failure → 502 BAD_GATEWAY")
        void restClientException_throws502() {
            stubOneStudent();
            when(restTemplate.postForObject(eq(RUN_URL), any(), eq(MLClusteringResponse.class)))
                    .thenThrow(new RestClientException("500 from microservice"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.runClustering(null));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            verify(assignmentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("persistAssignments() — the upsert")
    class PersistAssignments {

        @Test
        @DisplayName("Null assignments list → nothing saved, response still returned")
        void nullAssignments_savesNothing() {
            stubOneStudent();
            MLClusteringResponse resp = response(2, null, null);
            stubMicroserviceReturns(resp);

            assertSame(resp, service.runClustering(null));
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Empty assignments list → nothing saved")
        void emptyAssignments_savesNothing() {
            stubOneStudent();
            stubMicroserviceReturns(response(2, List.of(), List.of()));

            service.runClustering(null);

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("New user → a fresh assignment is created with clusterId, modelK and assignedAt")
        void newUser_createsAndSaves() {
            stubOneStudent();
            stubMicroserviceReturns(response(4,
                    List.of(assignment(1L, 2)),
                    List.of(summary(2, "Movers", 0.7, List.of("E1", "E2")))));
            when(assignmentRepository.findByUserId(1L)).thenReturn(Optional.empty());

            service.runClustering(null);

            StudentClusterAssignment saved = captureSaved();
            assertEquals(Long.valueOf(1L), saved.getUserId());
            assertEquals(Integer.valueOf(2), saved.getClusterId());
            assertEquals(Integer.valueOf(4), saved.getModelK());
            assertNotNull(saved.getAssignedAt());
        }

        @Test
        @DisplayName("Existing user → the SAME row is updated (upsert, not a duplicate insert)")
        void existingUser_upsertsSameEntity() {
            stubOneStudent();
            StudentClusterAssignment existing = new StudentClusterAssignment(1L);
            existing.setId(42L);
            existing.setClusterId(0);
            when(assignmentRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
            stubMicroserviceReturns(response(3,
                    List.of(assignment(1L, 1)),
                    List.of(summary(1, "Climbers", 0.6, List.of("E")))));

            service.runClustering(null);

            StudentClusterAssignment saved = captureSaved();
            assertSame(existing, saved);
            assertEquals(Long.valueOf(42L), saved.getId());
            assertEquals(Integer.valueOf(1), saved.getClusterId());
        }

        @Test
        @DisplayName("Assignments with a null userId or clusterId are skipped")
        void skipsNullUserIdOrClusterId() {
            stubOneStudent();
            stubMicroserviceReturns(response(2,
                    List.of(assignment(null, 1), assignment(7L, null)),
                    List.of()));

            service.runClustering(null);

            verify(assignmentRepository, never()).save(any());
            verify(assignmentRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("Cluster summary enriches the row: label, avg accuracy, comma-joined top errors")
        void enrichesFromSummary() {
            stubOneStudent();
            stubMicroserviceReturns(response(2,
                    List.of(assignment(1L, 0)),
                    List.of(summary(0, "Strugglers", 0.33, List.of("CARRY", "BORROW")))));
            when(assignmentRepository.findByUserId(1L)).thenReturn(Optional.empty());

            service.runClustering(null);

            StudentClusterAssignment saved = captureSaved();
            assertEquals("Strugglers", saved.getClusterLabel());
            assertEquals(Double.valueOf(0.33), saved.getClusterAvgAccuracy());
            assertEquals("CARRY,BORROW", saved.getClusterTopErrors());
        }

        @Test
        @DisplayName("Null top-error list → top errors stored as null")
        void nullTopErrors_setsNull() {
            stubOneStudent();
            stubMicroserviceReturns(response(2,
                    List.of(assignment(1L, 0)),
                    List.of(summary(0, "Strugglers", 0.33, null))));
            when(assignmentRepository.findByUserId(1L)).thenReturn(Optional.empty());

            service.runClustering(null);

            assertNull(captureSaved().getClusterTopErrors());
        }

        @Test
        @DisplayName("No matching cluster summary → row saved without enrichment fields")
        void noMatchingSummary_savesWithoutEnrichment() {
            stubOneStudent();
            stubMicroserviceReturns(response(2,
                    List.of(assignment(1L, 9)),     // clusterId 9 has no summary
                    List.of(summary(0, "Other", 0.5, List.of("E")))));
            when(assignmentRepository.findByUserId(1L)).thenReturn(Optional.empty());

            service.runClustering(null);

            StudentClusterAssignment saved = captureSaved();
            assertEquals(Integer.valueOf(9), saved.getClusterId());
            assertNull(saved.getClusterLabel());
            assertNull(saved.getClusterAvgAccuracy());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAssignmentForUser() — delegation")
    class GetAssignmentForUser {

        @Test
        @DisplayName("Returns the repository's value when present")
        void returnsPresentAssignment() {
            StudentClusterAssignment a = new StudentClusterAssignment(8L);
            when(assignmentRepository.findByUserId(8L)).thenReturn(Optional.of(a));

            Optional<StudentClusterAssignment> result = service.getAssignmentForUser(8L);

            assertTrue(result.isPresent());
            assertSame(a, result.get());
        }

        @Test
        @DisplayName("Returns empty when the user has no assignment")
        void returnsEmptyWhenAbsent() {
            when(assignmentRepository.findByUserId(8L)).thenReturn(Optional.empty());

            assertTrue(service.getAssignmentForUser(8L).isEmpty());
        }
    }
}
