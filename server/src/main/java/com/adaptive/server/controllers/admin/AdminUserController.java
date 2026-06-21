package com.adaptive.server.controllers.admin;

import com.adaptive.server.DTOs.AdminRoleUpdateRequest;
import com.adaptive.server.DTOs.AdminUserDto;
import com.adaptive.server.DTOs.PagedUsersResponse;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.admin.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final SessionValidationService sessionValidationService;

    public AdminUserController(AdminUserService adminUserService,
                              SessionValidationService sessionValidationService) {
        this.adminUserService = adminUserService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping
    public ResponseEntity<PagedUsersResponse> listUsers(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {

        sessionValidationService.validateAdminOnly(sessionToken);
        return ResponseEntity.ok(adminUserService.getUsers(page, size, search, role));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserDto> changeRole(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody AdminRoleUpdateRequest request) {
        SessionToken admin = sessionValidationService.validateAdminOnly(sessionToken);
        return ResponseEntity.ok(adminUserService.changeRole(admin.getUser().getId(), id, request.getRole()));
    }
}
