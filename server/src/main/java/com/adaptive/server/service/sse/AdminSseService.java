package com.adaptive.server.service.sse;

import com.adaptive.server.service.admin.AdminAnalyticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AdminSseService {

    private static final Logger log = LoggerFactory.getLogger(AdminSseService.class);
    private static final long EMITTER_TIMEOUT_MS = 300_000L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AdminAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public AdminSseService(AdminAnalyticsService analyticsService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("Admin SSE subscribed. Active admin connections: {}", emitters.size());
        return emitter;
    }

    public void pushAnalyticsUpdate() {
        if (emitters.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(analyticsService.getAnalytics());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize analytics for SSE push", e);
            return;
        }
        send(SseEmitter.event().name("analytics").data(json));
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        send(SseEmitter.event().comment("heartbeat"));
    }

    private void send(SseEmitter.SseEventBuilder event) {
        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
