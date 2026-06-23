package com.adaptive.server.controllers;

import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.sse.AdminSseService;
import com.adaptive.server.service.sse.UserSseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {

    private final AdminSseService adminSseService;
    private final UserSseService userSseService;
    private final SessionValidationService sessionValidationService;

    public SseController(AdminSseService adminSseService,
                         UserSseService userSseService,
                         SessionValidationService sessionValidationService) {
        this.adminSseService = adminSseService;
        this.userSseService = userSseService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping(value = "/sse/admin", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAdmin(
            @CookieValue(value = "session_token", required = false) String token) {
        sessionValidationService.validateAdminOnly(token);
        return adminSseService.subscribe();
    }

    @GetMapping(value = "/sse/user", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeUser(
            @CookieValue(value = "session_token", required = false) String token) {
        sessionValidationService.validateAndGetUser(token);
        return userSseService.subscribe();
    }

    @PostMapping("/admin/broadcast")
    public ResponseEntity<Void> broadcast(
            @CookieValue(value = "session_token", required = false) String token,
            @RequestBody BroadcastRequest body) {
        sessionValidationService.validateAdminOnly(token);
        userSseService.sendBroadcast(body.getMessage());
        return ResponseEntity.ok().build();
    }

    public static class BroadcastRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
