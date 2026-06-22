package com.adaptive.server.service.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UserSseService {

    private static final Logger log = LoggerFactory.getLogger(UserSseService.class);
    private static final long EMITTER_TIMEOUT_MS = 300_000L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("User SSE subscribed. Active user connections: {}", emitters.size());
        return emitter;
    }

    public void sendBroadcast(String message) {
        log.info("Broadcasting message to {} connected user(s)", emitters.size());
        send(SseEmitter.event().name("broadcast").data(message));
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        send(SseEmitter.event().comment("heartbeat"));
    }

    private void send(SseEmitter.SseEventBuilder event) {
        List<SseEmitter> dead = new ArrayList<>();
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
