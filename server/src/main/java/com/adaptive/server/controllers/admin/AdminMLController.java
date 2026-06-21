package com.adaptive.server.controllers.admin;

import com.adaptive.server.DTOs.AdminMLGroupsResponse;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.admin.AdminMLClusteringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only Admin view of existing clustering groups. Guarded by validateAdminOnly.
 * Does not run clustering — see MLClusteringController for the (admin-guarded) run.
 */
@RestController
@RequestMapping("/admin/ml-groups")
public class AdminMLController {

    private final AdminMLClusteringService adminMLClusteringService;
    private final SessionValidationService sessionValidationService;

    public AdminMLController(AdminMLClusteringService adminMLClusteringService,
                            SessionValidationService sessionValidationService) {
        this.adminMLClusteringService = adminMLClusteringService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping
    public ResponseEntity<AdminMLGroupsResponse> getGroups(
            @CookieValue(value = "session_token", required = false) String token) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminMLClusteringService.getGroups());
    }
}
