package com.adaptive.server.service;

import com.adaptive.server.entity.User;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.DailyBonusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Grants the daily-practice completion bonus (100 stars) and is the single source of
// truth for who may receive it. Secure by construction:
//   • the userId is taken from the validated session by the controller — never from the
//     request body, so a student can only claim for themselves;
//   • the "solved 5 today" requirement is re-verified from the server's own attempt log
//     (ExerciseAttempt), so a forged client call cannot earn the bonus without the work;
//   • idempotent — at most once per calendar day per student (lastDailyBonusDate guard),
//     so the endpoint cannot be replayed to farm stars.
@Service
public class DailyBonusService {

    public static final int DAILY_BONUS_STARS = 100;
    public static final int DAILY_GOAL = 5;

    private final UserRepository userRepository;
    private final ExerciseAttemptRepository attemptRepository;

    public DailyBonusService(UserRepository userRepository, ExerciseAttemptRepository attemptRepository) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public DailyBonusResponse claimDailyBonus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate today = LocalDate.now();
        long correctToday = attemptRepository
                .countByUserIdAndIsCorrectTrueAndAnsweredAtGreaterThanEqual(userId, today.atStartOfDay());
        int correct = (int) Math.min(correctToday, DAILY_GOAL);

        boolean alreadyClaimed = today.equals(user.getLastDailyBonusDate());
        boolean earned = correctToday >= DAILY_GOAL;

        if (!alreadyClaimed && earned) {
            user.setTotalStars(user.getTotalStars() + DAILY_BONUS_STARS);
            user.setLastDailyBonusDate(today);
            userRepository.save(user);
            return new DailyBonusResponse(true, user.getTotalStars(), DAILY_BONUS_STARS, correct, DAILY_GOAL);
        }

        // Already claimed today, or the 5-correct goal hasn't been met — no stars granted.
        return new DailyBonusResponse(false, user.getTotalStars(), DAILY_BONUS_STARS, correct, DAILY_GOAL);
    }
}
