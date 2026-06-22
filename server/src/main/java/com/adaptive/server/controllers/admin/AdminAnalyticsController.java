package com.adaptive.server.controllers.admin;

import com.adaptive.server.DTOs.AdminAnalyticsResponse;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.admin.AdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only platform analytics for admins. Guarded by validateAdminOnly.
 */
@RestController
@RequestMapping("/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final SessionValidationService sessionValidationService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService,
                                   SessionValidationService sessionValidationService) {
        this.adminAnalyticsService = adminAnalyticsService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics(
            @CookieValue(value = "session_token", required = false) String token) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminAnalyticsService.getAnalytics());
    }
}
