package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.service.LevelManagerService;
import com.adaptive.server.service.SessionValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.adaptive.server.responses.QuestionResponse;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final LevelManagerService levelManagerService;
    private final SessionValidationService sessionValidationService;

    public ProgressController(LevelManagerService levelManagerService,
                              SessionValidationService sessionValidationService) {
        this.levelManagerService   = levelManagerService;
        this.sessionValidationService = sessionValidationService;
    }

    @PostMapping("/submit-answer")
    public ResponseEntity<ProgressStatusResponse> submitAnswer(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody SubmitAnswerRequest request) {

        SessionToken token  = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.submitAnswer(userId, request);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/status/sub-subject/{subSubjectId}")
    public ResponseEntity<ProgressStatusResponse> getStatus(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long subSubjectId) {

        SessionToken token  = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.getStatus(userId, subSubjectId);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/next-question")
    public ResponseEntity<QuestionResponse> getNextQuestion(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam Long subSubjectId,
            @RequestParam(defaultValue = "he") String language,
            @RequestParam(defaultValue = "false") boolean mc) {

        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        QuestionResponse result = levelManagerService.getNextQuestion(
                userId, subSubjectId, language, mc);
        return ResponseEntity.ok(result);
    }
}

