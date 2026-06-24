package com.adaptive.server.service;

import com.adaptive.server.DTOs.StartBattleResponse;
import com.adaptive.server.entity.GamePlay;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.GamePlayRepository;
import com.adaptive.server.repository.StudentProgressRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.QuestionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Student games. Owns the secure star economy for entering a game (mirrors the avatar store's
 * server-side deduction) and the battle setup. A game NEVER touches level progression / rewards:
 * it only READS the student's level for difficulty, and gives a badge (recorded in GamePlay).
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    public static final int GAME_ENTRY_COST = 200;
    public static final int PLAYER_MAX_LIVES = 5;
    public static final String GAME_KEY_GALAXY_BATTLE = "galaxy-battle";
    public static final String SPEND_REASON_GAME_ENTRY = "GAME_ENTRY";
    private static final int QUESTIONS_PER_BATTLE = 20; // generous buffer; a battle never needs more

    private final UserRepository userRepository;
    private final StudentProgressRepository progressRepository;
    private final GamePlayRepository gamePlayRepository;
    private final LevelManagerService levelManagerService;

    public GameService(UserRepository userRepository,
                       StudentProgressRepository progressRepository,
                       GamePlayRepository gamePlayRepository,
                       LevelManagerService levelManagerService) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.gamePlayRepository = gamePlayRepository;
        this.levelManagerService = levelManagerService;
    }

    // Monster HP scales with the student's level in the chosen topic (easy → tough).
    static int monsterHpForLevel(int level) {
        if (level <= 2) return 5;
        if (level <= 4) return 8;
        return 12;
    }

    @Transactional
    public StartBattleResponse startBattle(User user, Long subSubjectId, String language) {
        // 1. Secure, server-side star deduction (same authoritative pattern as the avatar store).
        if (user.getTotalStars() < GAME_ENTRY_COST) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Not enough stars");
        }
        user.setTotalStars(user.getTotalStars() - GAME_ENTRY_COST);
        userRepository.save(user);
        log.info("{} — user {} spent {} stars on {}", SPEND_REASON_GAME_ENTRY, user.getId(),
                GAME_ENTRY_COST, GAME_KEY_GALAXY_BATTLE);

        // 2. Difficulty comes from the student's level in this topic (set by the placement survey,
        //    updated by practice). Default 1 if they have not practised it yet. READ-ONLY.
        int level = progressRepository.findByUserIdAndSubSubjectId(user.getId(), subSubjectId)
                .map(StudentProgress::getCurrentLevel)
                .orElse(1);

        // 3. Battle questions via the EXISTING generator — no progress / level side effects.
        String lang = language != null ? language : "he";
        List<QuestionResponse> questions = new ArrayList<>();
        for (int i = 0; i < QUESTIONS_PER_BATTLE; i++) {
            questions.add(levelManagerService.generateGameQuestion(subSubjectId, level, lang, true, user.getFullName()));
        }

        return new StartBattleResponse(user.getTotalStars(), level,
                monsterHpForLevel(level), PLAYER_MAX_LIVES, questions);
    }

    @Transactional
    public void finishBattle(User user, Long subSubjectId, Integer level, boolean won) {
        // History + badge only. Explicitly does NOT change StudentProgress / level progression.
        gamePlayRepository.save(new GamePlay(user, GAME_KEY_GALAXY_BATTLE, subSubjectId, level,
                won, GAME_ENTRY_COST, LocalDateTime.now()));
    }
}
