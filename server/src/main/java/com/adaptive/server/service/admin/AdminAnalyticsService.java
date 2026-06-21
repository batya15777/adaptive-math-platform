package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminAnalyticsResponse;
import com.adaptive.server.DTOs.AdminAnalyticsSummaryDto;
import com.adaptive.server.DTOs.AdminErrorStatDto;
import com.adaptive.server.DTOs.AdminLeaderboardEntryDto;
import com.adaptive.server.DTOs.AdminSubSubjectStatDto;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only platform analytics for the Admin dashboard. Aggregates existing data
 * (attempts, users) with single GROUP BY / count queries. Does NOT touch question
 * generation, templates, the ML microservice, or write anything.
 */
@Service
public class AdminAnalyticsService {

    private static final String STUDENT_ROLE = "STUDENT";
    private static final int MIN_ATTEMPTS = 5;          // hide sub-subjects with too little data
    private static final int HARDEST_LIMIT = 10;
    private static final int COMMON_ERRORS_LIMIT = 10;
    private static final int ACTIVE_WINDOW_DAYS = 7;

    private final ExerciseAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    public AdminAnalyticsService(ExerciseAttemptRepository attemptRepository,
                                 UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        // All learning metrics are scoped to users whose CURRENT role is STUDENT,
        // so ex-students who became admins don't skew the numbers.
        long totalAttempts = attemptRepository.countAttemptsByUserRole(STUDENT_ROLE);
        long correct = attemptRepository.countCorrectAttemptsByUserRole(STUDENT_ROLE);
        int successRate = totalAttempts > 0 ? (int) Math.round(100.0 * correct / totalAttempts) : 0;
        long activeLearners = attemptRepository.countDistinctActiveStudentsSince(
                STUDENT_ROLE, LocalDateTime.now().minusDays(ACTIVE_WINDOW_DAYS));

        AdminAnalyticsSummaryDto summary = new AdminAnalyticsSummaryDto(
                userRepository.count(),
                userRepository.countByRole(STUDENT_ROLE),
                totalAttempts,
                successRate,
                activeLearners);

        // Hardest learning areas: only those with enough attempts, lowest success first.
        List<AdminSubSubjectStatDto> hardestSubSubjects = attemptRepository.findSubSubjectStatsForRole(STUDENT_ROLE).stream()
                .filter(p -> p.getTotal() != null && p.getTotal() >= MIN_ATTEMPTS)
                .map(p -> {
                    long total = p.getTotal();
                    long c = p.getCorrect() == null ? 0L : p.getCorrect();
                    int rate = total > 0 ? (int) Math.round(100.0 * c / total) : 0;
                    return new AdminSubSubjectStatDto(p.getSubSubjectId(), p.getSubSubjectName(), total, c, rate);
                })
                .sorted(Comparator.comparingInt(AdminSubSubjectStatDto::getSuccessRate))
                .limit(HARDEST_LIMIT)
                .collect(Collectors.toList());

        List<AdminErrorStatDto> commonErrors = attemptRepository.findErrorPatternCountsForRole(STUDENT_ROLE).stream()
                .limit(COMMON_ERRORS_LIMIT)
                .map(p -> new AdminErrorStatDto(p.getErrorPattern(),
                        p.getOccurrences() == null ? 0L : p.getOccurrences()))
                .collect(Collectors.toList());

        List<AdminLeaderboardEntryDto> leaderboard = userRepository.findTop10ByRoleOrderByTotalStarsDesc(STUDENT_ROLE).stream()
                .map(u -> new AdminLeaderboardEntryDto(u.getId(), u.getFullName(),
                        u.getTotalStars() == null ? 0 : u.getTotalStars()))
                .collect(Collectors.toList());

        return new AdminAnalyticsResponse(summary, hardestSubSubjects, commonErrors, leaderboard);
    }
}
