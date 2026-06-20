package com.adaptive.server.controllers.admin;

import com.adaptive.server.service.SessionValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin area (phase 0).
 *
 * Convention for ALL future /admin/** endpoints: call
 * sessionValidationService.validateAdminOnly(sessionToken) before doing anything,
 * so non-admins get a 403 even if they bypass the client-side guard.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final SessionValidationService sessionValidationService;

    public AdminController(SessionValidationService sessionValidationService) {
        this.sessionValidationService = sessionValidationService;
    }

    // Minimal protected endpoint just to verify the admin guard works end-to-end.
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, String>> summary(
            @CookieValue(value = "session_token", required = false) String sessionToken) {

        sessionValidationService.validateAdminOnly(sessionToken);
        return ResponseEntity.ok(Map.of("status", "ok", "scope", "admin"));
    }
}
