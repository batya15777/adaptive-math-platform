package com.adaptive.server.controllers;

import com.adaptive.server.entity.BroadcastMessage;
import com.adaptive.server.repository.BroadcastMessageRepository;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.sse.AdminSseService;
import com.adaptive.server.service.sse.UserSseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SseController {

    private final AdminSseService adminSseService;
    private final UserSseService userSseService;
    private final SessionValidationService sessionValidationService;
    private final BroadcastMessageRepository broadcastRepo;

    public SseController(AdminSseService adminSseService,
                         UserSseService userSseService,
                         SessionValidationService sessionValidationService,
                         BroadcastMessageRepository broadcastRepo) {
        this.adminSseService = adminSseService;
        this.userSseService = userSseService;
        this.sessionValidationService = sessionValidationService;
        this.broadcastRepo = broadcastRepo;
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

    /** Returns admin broadcast messages from the last 7 days, newest first. */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @CookieValue(value = "session_token", required = false) String token) {
        sessionValidationService.validateAndGetUser(token);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<NotificationDTO> result = broadcastRepo
                .findBySentAtAfterOrderBySentAtDesc(cutoff)
                .stream()
                .map(m -> new NotificationDTO(m.getId(), m.getMessage(), m.getSentAt().toString()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    public static class BroadcastRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class NotificationDTO {
        public final Long id;
        public final String message;
        public final String sentAt;
        public NotificationDTO(Long id, String message, String sentAt) {
            this.id = id; this.message = message; this.sentAt = sentAt;
        }
    }
}
