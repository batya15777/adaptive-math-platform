package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.TutorChatHistoryMessage;
import com.adaptive.server.DTOs.TutorChatRequest;
import com.adaptive.server.DTOs.TutorChatResponse;
import com.adaptive.server.DTOs.TutorConversationSummary;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.TutorChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    // List of past conversations (one per question), newest first — for the history panel.
    @GetMapping("/conversations")
    public ResponseEntity<List<TutorConversationSummary>> conversations(
            @CookieValue(value = "session_token", required = false) String sessionToken) {

        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return ResponseEntity.ok(tutorChatService.getConversations(token.getUser()));
    }

    // Saved messages for one question — used to restore the chat and to view past conversations.
    @GetMapping("/{questionId}/messages")
    public ResponseEntity<List<TutorChatHistoryMessage>> history(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long questionId) {

        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return ResponseEntity.ok(tutorChatService.getHistory(token.getUser(), questionId));
    }
}
