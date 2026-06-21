package com.adaptive.server.controllers.admin;

import com.adaptive.server.DTOs.AdminTopicDto;
import com.adaptive.server.DTOs.AdminTopicRequest;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.admin.AdminTopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin content management for Topics. Every endpoint is guarded by
 * {@code validateAdminOnly}. There is intentionally NO delete endpoint —
 * topics are disabled via {@code PUT /{id}/active}, not removed.
 */
@RestController
@RequestMapping("/admin/content/topics")
public class AdminTopicController {

    private final AdminTopicService adminTopicService;
    private final SessionValidationService sessionValidationService;

    public AdminTopicController(AdminTopicService adminTopicService,
                               SessionValidationService sessionValidationService) {
        this.adminTopicService = adminTopicService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping
    public ResponseEntity<List<AdminTopicDto>> list(
            @CookieValue(value = "session_token", required = false) String token) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminTopicService.getTopics());
    }

    @PostMapping
    public ResponseEntity<AdminTopicDto> create(
            @CookieValue(value = "session_token", required = false) String token,
            @RequestBody AdminTopicRequest request) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminTopicService.create(request.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminTopicDto> rename(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id, @RequestBody AdminTopicRequest request) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminTopicService.rename(id, request.getName()));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<AdminTopicDto> setActive(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id, @RequestParam boolean active) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminTopicService.setActive(id, active));
    }
}
