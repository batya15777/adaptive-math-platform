package com.adaptive.server.controllers.admin;

import com.adaptive.server.DTOs.AdminSubSubjectDto;
import com.adaptive.server.DTOs.AdminSubSubjectRequest;
import com.adaptive.server.DTOs.AdminSubjectSubSubjectsDto;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.admin.AdminSubSubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin content management for SubSubjects. Every endpoint is guarded by
 * validateAdminOnly. No delete endpoint — sub-subjects are disabled, not removed.
 */
@RestController
@RequestMapping("/admin/content")
public class AdminSubSubjectController {

    private final AdminSubSubjectService adminSubSubjectService;
    private final SessionValidationService sessionValidationService;

    public AdminSubSubjectController(AdminSubSubjectService adminSubSubjectService,
                                    SessionValidationService sessionValidationService) {
        this.adminSubSubjectService = adminSubSubjectService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping("/subjects/{subjectId}/sub-subjects")
    public ResponseEntity<AdminSubjectSubSubjectsDto> list(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long subjectId) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubSubjectService.getBySubject(subjectId));
    }

    @PostMapping("/subjects/{subjectId}/sub-subjects")
    public ResponseEntity<AdminSubSubjectDto> create(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long subjectId, @RequestBody AdminSubSubjectRequest request) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubSubjectService.create(subjectId, request.getName()));
    }

    @PutMapping("/sub-subjects/{id}")
    public ResponseEntity<AdminSubSubjectDto> rename(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id, @RequestBody AdminSubSubjectRequest request) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubSubjectService.rename(id, request.getName()));
    }

    @PutMapping("/sub-subjects/{id}/active")
    public ResponseEntity<AdminSubSubjectDto> setActive(
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id, @RequestParam boolean active) {
        sessionValidationService.validateAdminOnly(token);
        return ResponseEntity.ok(adminSubSubjectService.setActive(id, active));
    }
}
