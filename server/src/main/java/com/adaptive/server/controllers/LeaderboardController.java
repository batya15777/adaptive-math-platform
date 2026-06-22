package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.LeaderboardEntryDto;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.LeaderboardResponse;
import com.adaptive.server.service.SessionValidationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final UserRepository userRepository;
    private final SessionValidationService sessionValidationService;

    public LeaderboardController(UserRepository userRepository, SessionValidationService sessionValidationService) {
        this.userRepository = userRepository;
        this.sessionValidationService = sessionValidationService;
    }


    @GetMapping("/top10")//זה בעצם 10 תלמידים שיש להם הכי הרבה כוכבים נכנסו ל10 הכי טובים
    public ResponseEntity<LeaderboardResponse> getTop10Stars(
            @CookieValue(value = "session_token", required = false) String sessionToken) {

        sessionValidationService.validateAndGetUser(sessionToken);
        // Single query: top-10 by stars, each row carrying the user's avatar (pictureId).
        List<LeaderboardEntryDto> leaderboardEntries =
                userRepository.findTopWithAvatar(PageRequest.of(0, 10));
        LeaderboardResponse response = new LeaderboardResponse(
                true,
                "Top 10 leaderboard fetched successfully.",
                leaderboardEntries
        );
        return ResponseEntity.ok(response);
    }
}
