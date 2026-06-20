package com.adaptive.server.service;

import com.adaptive.server.entity.StudentClusterAssignment;
import com.adaptive.server.repository.StudentClusterAssignmentRepository;
import com.adaptive.server.service.QuestionsGenerators.ClusterContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a {@link ClusterContext} for a student from their persisted
 * {@link StudentClusterAssignment}. This is the single place the question
 * generators get cluster info from, keeping clustering concerns out of the
 * generators themselves.
 */
@Service
public class ClusterContextService {

    // Accuracy thresholds that translate a cohort's average accuracy into a
    // difficulty nudge for the next question.
    private static final double STRONG_ACCURACY = 0.80;
    private static final double WEAK_ACCURACY = 0.40;

    private final StudentClusterAssignmentRepository assignmentRepository;

    public ClusterContextService(StudentClusterAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public ClusterContext forUser(Long userId) {
        return assignmentRepository.findByUserId(userId)
                .map(this::toContext)
                .orElseGet(ClusterContext::neutral);
    }

    private ClusterContext toContext(StudentClusterAssignment assignment) {
        List<String> topErrors = parseTopErrors(assignment.getClusterTopErrors());
        int bias = biasFromAccuracy(assignment.getClusterAvgAccuracy());
        return ClusterContext.of(assignment.getClusterId(), assignment.getClusterLabel(), topErrors, bias);
    }

    private List<String> parseTopErrors(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Strong cohorts get slightly harder questions; struggling cohorts get easier ones.
    private int biasFromAccuracy(Double avgAccuracy) {
        if (avgAccuracy == null) {
            return 0;
        }
        if (avgAccuracy >= STRONG_ACCURACY) {
            return 1;
        }
        if (avgAccuracy < WEAK_ACCURACY) {
            return -1;
        }
        return 0;
    }
}
