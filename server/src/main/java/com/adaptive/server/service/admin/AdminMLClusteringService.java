package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminMLGroupDto;
import com.adaptive.server.DTOs.AdminMLGroupsResponse;
import com.adaptive.server.repository.StudentClusterAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only view of the clustering results already stored in
 * student_cluster_assignments. Does NOT run clustering, call the microservice,
 * or touch the ML logic — it only aggregates existing rows for the admin screen.
 */
@Service
public class AdminMLClusteringService {

    private final StudentClusterAssignmentRepository assignmentRepository;

    public AdminMLClusteringService(StudentClusterAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public AdminMLGroupsResponse getGroups() {
        List<AdminMLGroupDto> groups = assignmentRepository.findClusterGroups().stream()
                .map(p -> new AdminMLGroupDto(
                        p.getClusterId(),
                        p.getLabel(),
                        p.getMemberCount() == null ? 0L : p.getMemberCount(),
                        p.getAvgAccuracy(),                 // nullable — client renders "—"
                        parseTopErrors(p.getTopErrors()),   // never null
                        p.getModelK(),
                        p.getLastAssignedAt()))             // nullable — client renders "—"
                .collect(Collectors.toList());

        return new AdminMLGroupsResponse(
                assignmentRepository.findLastRunAt(),       // null when empty
                assignmentRepository.count(),               // 0 when empty
                groups);                                    // [] when empty
    }

    // Comma-joined error patterns → list; null/blank → empty list (no crash).
    private List<String> parseTopErrors(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
