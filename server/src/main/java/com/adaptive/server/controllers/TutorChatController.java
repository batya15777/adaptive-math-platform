package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.TutorChatRequest;
import com.adaptive.server.DTOs.TutorChatResponse;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.TutorChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor-chat")
public class TutorChatController {

    private final TutorChatService tutorChatService;
    private final SessionValidationService sessionValidationService;

    public TutorChatController(TutorChatService tutorChatService,
                               SessionValidationService sessionValidationService) {
        this.tutorChatService = tutorChatService;
        this.sessionValidationService = sessionValidationService;
    }

    @PostMapping
    public ResponseEntity<TutorChatResponse> chat(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody TutorChatRequest request) {

        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        TutorChatResponse response = tutorChatService.askTutor(token.getUser(), request);

        return ResponseEntity.ok(response);
    }
}
