package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.LeaderboardEntryDto;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.LeaderboardResponse;
import com.adaptive.server.service.SessionValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<User> topUsers = userRepository.findTop10ByOrderByTotalStarsDesc();
        // יצירת רשימה ריקה והוספת איברים בלולאה
        List<LeaderboardEntryDto> leaderboardEntries = new ArrayList<>();
        for (User user : topUsers) {
            LeaderboardEntryDto entry = new LeaderboardEntryDto(user.getFullName(), user.getTotalStars());
            leaderboardEntries.add(entry);
        }
        LeaderboardResponse response = new LeaderboardResponse(
                true,
                "Top 10 leaderboard fetched successfully.",
                leaderboardEntries
        );
        return ResponseEntity.ok(response);
    }
}
