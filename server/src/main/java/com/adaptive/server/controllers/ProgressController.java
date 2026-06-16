package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.InitialAssessmentRequest;
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

    private final LevelManagerService     levelManagerService;
    private final SessionValidationService sessionValidationService;

    public ProgressController(LevelManagerService levelManagerService,
                               SessionValidationService sessionValidationService) {
        this.levelManagerService    = levelManagerService;
        this.sessionValidationService = sessionValidationService;
    }

    /**
     * POST /progress/submit-answer
     *
     * Records the student's answer for a normal practice question and returns the
     * updated progress status.  If the student just levelled up the response will
     * have {@code bonusQuestionTriggered=true} — the frontend should then call
     * {@code GET /progress/bonus-question} to fetch the bonus challenge.
     */
    @PostMapping("/submit-answer")
    public ResponseEntity<ProgressStatusResponse> submitAnswer(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody SubmitAnswerRequest request) {
        SessionToken token  = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId         = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.submitAnswer(userId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /progress/status/sub-subject/{subSubjectId}
     *
     * Returns the student's current progress status for a given sub-subject
     * without recording any new attempt.
     */
    @GetMapping("/status/sub-subject/{subSubjectId}")
    public ResponseEntity<ProgressStatusResponse> getStatus(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long subSubjectId) {
        SessionToken token  = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId         = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.getStatus(userId, subSubjectId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /progress/next-question
     *
     * Generates and returns the next adaptive practice question for the student.
     * The generator selects the operation and difficulty based on the student's
     * current level and any detected weakness.
     *
     * @param subSubjectId  ID of the sub-subject to practice
     * @param language      preferred question language (default: "he")
     * @param mc            if {@code true}, returns a multiple-choice question
     */
    @GetMapping("/next-question")
    public ResponseEntity<QuestionResponse> getNextQuestion(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam Long subSubjectId,
            @RequestParam(defaultValue = "he") String language,
            @RequestParam(defaultValue = "false") boolean mc) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId        = token.getUser().getId();
        QuestionResponse result = levelManagerService.getNextQuestion(userId, subSubjectId, language, mc);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /progress/bonus-question
     *
     * Fetches a bonus question to present after the student levels up.
     * The question is always from the "mixed" (compound-operator) sub-subject and
     * its difficulty is scaled to {@code min(currentLevel + 2, 10)} to ensure a
     * genuine challenge.
     *
     * <p>Call this endpoint only when {@code submitAnswer} returns
     * {@code bonusQuestionTriggered=true}.
     *
     * @param subSubjectId  the sub-subject the student was practising (used to look up their level)
     * @param language      preferred question language (default: "he")
     */
    @GetMapping("/bonus-question")
    public ResponseEntity<BonusQuestionResponse> getBonusQuestion(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam Long subSubjectId,
            @RequestParam(defaultValue = "he") String language) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId        = token.getUser().getId();
        BonusQuestionResponse result = levelManagerService.getBonusQuestion(userId, subSubjectId, language);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /progress/bonus-answer
     *
     * Records the student's answer for a bonus question.  If correct, exactly 50 stars
     * are added to the student's global {@code totalStars} count and persisted to the DB.
     *
     * @param subSubjectId  the sub-subject associated with the bonus question
     * @param correct       {@code true} if the student answered correctly
     */
    @PostMapping("/bonus-answer")
    public ResponseEntity<ProgressStatusResponse> submitBonusAnswer(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam Long subSubjectId,
            @RequestParam boolean correct) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId        = token.getUser().getId();
        ProgressStatusResponse result = levelManagerService.submitBonusAnswer(userId, subSubjectId, correct);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /progress/initial-assessment
     *
     * Processes an initial assessment questionnaire from a new user to determine
     * their starting sub-subject level and difficulty level. Generates and returns
     * an appropriate initial practice question.
     */
    @PostMapping("/initial-assessment")
    public ResponseEntity<QuestionResponse> initialAssessment(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody InitialAssessmentRequest request) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        Long userId        = token.getUser().getId();
        QuestionResponse result = levelManagerService.getInitialAssessmentQuestion(userId, request);
        return ResponseEntity.ok(result);
    }
}
