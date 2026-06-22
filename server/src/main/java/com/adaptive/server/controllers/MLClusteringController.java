package com.adaptive.server.controllers;

import com.adaptive.server.responses.MLClusteringResponse;
import com.adaptive.server.service.MLClusteringService;
import com.adaptive.server.service.SessionValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ml/clustering")
public class MLClusteringController {
    private final MLClusteringService mlClusteringService;
    private final SessionValidationService sessionValidationService;

    public MLClusteringController(MLClusteringService mlClusteringService, SessionValidationService sessionValidationService) {
        this.mlClusteringService = mlClusteringService;
        this.sessionValidationService = sessionValidationService;
    }


    @PostMapping("/run")
    public ResponseEntity<MLClusteringResponse> run(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestParam(value = "k", required = false) Integer k) {
        sessionValidationService.validateAdminOnly(sessionToken);
        return ResponseEntity.ok(mlClusteringService.runClustering(k));
    }
}
