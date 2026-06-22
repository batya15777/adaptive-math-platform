package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.InitialAssessmentRequest;
import com.adaptive.server.DTOs.RevealSolutionRequest;
import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.responses.BonusQuestionResponse;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.responses.QuestionResponse;
import com.adaptive.server.service.LevelManagerService;
import com.adaptive.server.service.SessionValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final LevelManagerService levelManagerService;
    private final SessionValidationService sessionValidationService;

    public ProgressController(LevelManagerService levelManagerService,
                               SessionValidationService sessionValidationService) {
        this.levelManagerService = levelManagerService;
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


    @PostMapping("/reveal-solution")//הצגת הפתרון המלא — מסמן את השאלה כ-FAILED
    public ResponseEntity<ProgressStatusResponse> revealSolution(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody RevealSolutionRequest request) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.revealSolution(
                userId, request.getSubSubjectId(), request.getQuestionId());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/status/sub-subject/{subSubjectId}")//מצב בתת נושא
    public ResponseEntity<ProgressStatusResponse> getStatus(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long subSubjectId) {
        SessionToken token  = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.getStatus(userId, subSubjectId);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/next-question")//שאלה הבאה
    public ResponseEntity<QuestionResponse> getNextQuestion(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam Long subSubjectId,
            @RequestParam(defaultValue = "he") String language,
            @RequestParam(defaultValue = "false") boolean mc) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        QuestionResponse result = levelManagerService.getNextQuestion(userId, subSubjectId, language, mc);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/bonus-question") //לקבל שאלת בונוס מהמחולל לפי איזה תת נושא תלמיד נמצא בו
    public ResponseEntity<BonusQuestionResponse> getBonusQuestion(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam Long subSubjectId,
            @RequestParam(defaultValue = "he") String language) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        BonusQuestionResponse result = levelManagerService.getBonusQuestion(userId, subSubjectId, language);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/bonus-answer") //לענות על בונוס — נשמר כ-attempt ומוזן ל-ML בדיוק כמו תשובה רגילה
    public ResponseEntity<ProgressStatusResponse> submitBonusAnswer(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody SubmitAnswerRequest request) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.submitBonusAnswer(userId, request);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/initial-assessment")//זה בעצם השאלון שמגדיר רמת תלמיד אחרי הרשמה
    public ResponseEntity<QuestionResponse> initialAssessment(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody InitialAssessmentRequest request) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId = token.getUser().getId();
        QuestionResponse result = levelManagerService.getInitialAssessmentQuestion(userId, request);
        return ResponseEntity.ok(result);
    }
}
