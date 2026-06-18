package com.adaptive.server.controllers;

import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.responses.SubSubjectProgressResponse;
import com.adaptive.server.responses.SubjectResponse;
import com.adaptive.server.responses.TopicResponse;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Selection endpoints for the Math Training page: topics → subjects → sub-subjects.
 * All require a valid session (the sub-subjects view needs the student's id for progress).
 */
@RestController
@RequestMapping("/training")
public class TrainingController {

    private final TrainingService trainingService;
    private final SessionValidationService sessionValidationService;

    public TrainingController(TrainingService trainingService,
                              SessionValidationService sessionValidationService) {
        this.trainingService          = trainingService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponse>> getTopics(
            @CookieValue(value = "session_token", required = false) String sessionToken) {
        resolveUser(sessionToken);
        return ResponseEntity.ok(trainingService.getTopics());
    }

    @GetMapping("/topics/{topicId}/subjects")
    public ResponseEntity<List<SubjectResponse>> getSubjects(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long topicId) {
        resolveUser(sessionToken);
        return ResponseEntity.ok(trainingService.getSubjects(topicId));
    }

    @GetMapping("/subjects/{subjectId}/sub-subjects")
    public ResponseEntity<List<SubSubjectProgressResponse>> getSubSubjects(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @PathVariable Long subjectId) {
        User user = resolveUser(sessionToken);
        return ResponseEntity.ok(trainingService.getSubSubjects(user.getId(), subjectId));
    }

    private User resolveUser(String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return token.getUser();
    }
}
