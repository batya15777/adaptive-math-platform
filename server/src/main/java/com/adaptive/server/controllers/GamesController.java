package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.FinishBattleRequest;
import com.adaptive.server.DTOs.StartBattleRequest;
import com.adaptive.server.DTOs.StartBattleResponse;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.service.GameService;
import com.adaptive.server.service.SessionValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Student games. Entry costs stars (deducted server-side); games never change level progression.
 */
@RestController
@RequestMapping("/games")
public class GamesController {

    private final GameService gameService;
    private final SessionValidationService sessionValidationService;

    public GamesController(GameService gameService, SessionValidationService sessionValidationService) {
        this.gameService = gameService;
        this.sessionValidationService = sessionValidationService;
    }

    // Spend GAME_ENTRY_COST stars + start a battle on the chosen topic, at the student's level.
    // 402 PAYMENT_REQUIRED when the student doesn't have enough stars.
    @PostMapping("/galaxy-battle/start")
    public ResponseEntity<StartBattleResponse> startBattle(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody StartBattleRequest request) {
        User user = resolveUser(sessionToken);
        return ResponseEntity.ok(gameService.startBattle(user, request.getSubSubjectId(), request.getLanguage()));
    }

    // Record the outcome (history + badge). Does NOT change level progression or grant stars.
    @PostMapping("/galaxy-battle/finish")
    public ResponseEntity<Void> finishBattle(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody FinishBattleRequest request) {
        User user = resolveUser(sessionToken);
        gameService.finishBattle(user, request.getSubSubjectId(), request.getLevel(), request.isWon());
        return ResponseEntity.ok().build();
    }

    private User resolveUser(String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return token.getUser();
    }
}
