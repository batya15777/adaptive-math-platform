package com.adaptive.server.controllers.admin;

import com.adaptive.server.DTOs.AdminSubjectDto;
import com.adaptive.server.DTOs.AdminSubjectRequest;
import com.adaptive.server.DTOs.AdminTopicSubjectsDto;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.admin.AdminSubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin content management for Subjects. Every endpoint is guarded by
 * validateAdminOnly. There is no delete endpoint — subjects are disabled, not removed.
 */
@RestController
@RequestMapping("/admin/content")
public class AdminSubjectController {

    private final AdminSubjectService adminSubjectService;
    private final SessionValidationService sessionValidationService;

    public AdminSubjectController(AdminSubjectService adminSubjectService,
                                 SessionValidationService sessionValidationService) {
        this.adminSubjectService = adminSubjectService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping("/topics/{topicId}/subjects")
    public ResponseEntity<AdminTopicSubjectsDto> list(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long topicId) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubjectService.getByTopic(topicId));
    }

    @PostMapping("/topics/{topicId}/subjects")
    public ResponseEntity<AdminSubjectDto> create(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long topicId, @RequestBody AdminSubjectRequest request) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubjectService.create(topicId, request.getName()));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<AdminSubjectDto> rename(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id, @RequestBody AdminSubjectRequest request) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubjectService.rename(id, request.getName()));
    }

    @PutMapping("/subjects/{id}/active")
    public ResponseEntity<AdminSubjectDto> setActive(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id, @RequestParam boolean active) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubjectService.setActive(id, active));
    }
}
