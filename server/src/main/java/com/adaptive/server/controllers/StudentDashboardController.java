package com.adaptive.server.controllers;

import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.responses.StudentDashboardResponse;
import com.adaptive.server.responses.StudentDashboardResponse.ClusterInfo;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.StudentDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

/**
 * Student-facing dashboard API. Every endpoint is scoped to the logged-in student
 * (resolved from the session cookie) — no admin role required, no cross-user access.
 */
@RestController
@RequestMapping("/api/student")
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;
    private final SessionValidationService sessionValidationService;

    public StudentDashboardController(StudentDashboardService dashboardService,
                                      SessionValidationService sessionValidationService) {
        this.dashboardService = dashboardService;
        this.sessionValidationService = sessionValidationService;
    }

    /** Full dashboard payload (stats, cluster, topics, recommendations, badges). */
    @GetMapping("/dashboard-data")
    public ResponseEntity<StudentDashboardResponse> getDashboardData(
            @CookieValue(value = "session_token", required = false) String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return ResponseEntity.ok(dashboardService.buildDashboard(token.getUser().getId()));
    }

    /** Lightweight cluster lookup — used by the Question Game's "adapted for you" badge. */
    @GetMapping("/my-cluster")
    public ResponseEntity<ClusterInfo> getMyCluster(
            @CookieValue(value = "session_token", required = false) String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return ResponseEntity.ok(dashboardService.getClusterInfo(token.getUser().getId()));
    }
}
