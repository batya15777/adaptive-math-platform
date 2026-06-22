package com.adaptive.server.service;

import com.adaptive.server.entity.StudentClusterAssignment;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.repository.*;
import com.adaptive.server.responses.StudentDashboardResponse;
import com.adaptive.server.responses.StudentDashboardResponse.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Builds the Student Learning Dashboard payload: it aggregates the student's
 * attempt history and their ML cluster into ready-to-render insights —
 * overall stats, per-topic progress, adaptive recommendations and badges.
 *
 * All "what does this mean / what should I do" logic lives here so the React
 * layer stays presentational.
 */
@Service
public class StudentDashboardService {

    private static final String CALCULATION_SUBJECT_NAME = "Calculation";
    private static final int DEFAULT_LEVEL = 1;

    private final UserRepository userRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final StudentProgressRepository progressRepository;
    private final StudentClusterAssignmentRepository clusterAssignmentRepository;
    private final SubSubjectRepository subSubjectRepository;

    public StudentDashboardService(UserRepository userRepository,
                                   ExerciseAttemptRepository attemptRepository,
                                   StudentProgressRepository progressRepository,
                                   StudentClusterAssignmentRepository clusterAssignmentRepository,
                                   SubSubjectRepository subSubjectRepository) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.clusterAssignmentRepository = clusterAssignmentRepository;
        this.subSubjectRepository = subSubjectRepository;
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse buildDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        Map<Long, Integer> levelBySubSubject = levelsFor(userId);
        List<TopicProgress> topics = buildTopics(userId, levelBySubSubject);
        OverallStats overall = buildOverall(topics, levelBySubSubject);
        ClusterInfo cluster = buildClusterInfo(userId);
        List<Recommendation> recommendations = buildRecommendations(userId, cluster);
        List<Badge> badges = buildBadges(overall, user.getTotalStars(), topics);

        StudentDashboardResponse response = new StudentDashboardResponse();
        response.setStudent(new StudentSummary(user.getId(), displayName(user), user.getTotalStars()));
        response.setOverall(overall);
        response.setCluster(cluster);
        response.setTopics(topics);
        response.setRecommendations(recommendations);
        response.setBadges(badges);
        return response;
    }

    /** Lightweight cluster lookup for the in-game "adapted for you" badge. */
    @Transactional(readOnly = true)
    public ClusterInfo getClusterInfo(Long userId) {
        return buildClusterInfo(userId);
    }

    // ── builders ─────────────────────────────────────────────────────────────

    private Map<Long, Integer> levelsFor(Long userId) {
        Map<Long, Integer> levels = new HashMap<>();
        for (StudentProgress p : progressRepository.findByUserId(userId)) {
            if (p.getSubSubject() != null) {
                levels.put(p.getSubSubject().getId(),
                        p.getCurrentLevel() == null ? DEFAULT_LEVEL : p.getCurrentLevel());
            }
        }
        return levels;
    }

    private List<TopicProgress> buildTopics(Long userId, Map<Long, Integer> levelBySubSubject) {
        List<TopicProgress> topics = new ArrayList<>();
        for (TopicStatsProjection s : attemptRepository.findTopicStatsByUser(userId)) {
            long total = s.getTotal() == null ? 0 : s.getTotal();
            long correct = s.getCorrect() == null ? 0 : s.getCorrect();
            int level = levelBySubSubject.getOrDefault(s.getSubSubjectId(), DEFAULT_LEVEL);
            topics.add(new TopicProgress(
                    s.getSubSubjectId(), s.getSubSubjectName(), total, correct,
                    percent(correct, total), level));
        }
        return topics;
    }

    private OverallStats buildOverall(List<TopicProgress> topics, Map<Long, Integer> levelBySubSubject) {
        long totalAttempts = topics.stream().mapToLong(TopicProgress::getAttempts).sum();
        long totalCorrect = topics.stream().mapToLong(TopicProgress::getCorrect).sum();
        // "Top level reached" = the highest progression level (current_level) the student has
        // climbed to across their sub-subjects. This is a LEVEL, not a difficulty — it mirrors the
        // cards' רמה stat (difficulty/קושי is the separate cluster-biased number).
        int masteryLevel = levelBySubSubject.values().stream().max(Integer::compareTo).orElse(DEFAULT_LEVEL);
        return new OverallStats(totalAttempts, totalCorrect, percent(totalCorrect, totalAttempts), masteryLevel);
    }

    private ClusterInfo buildClusterInfo(Long userId) {
        Optional<StudentClusterAssignment> assignmentOpt = clusterAssignmentRepository.findByUserId(userId);
        if (assignmentOpt.isEmpty()) {
            return ClusterInfo.unassigned();
        }
        StudentClusterAssignment a = assignmentOpt.get();
        ClusterInfo info = new ClusterInfo();
        info.setAssigned(true);
        info.setClusterId(a.getClusterId());
        info.setLabel(a.getClusterLabel());
        info.setAvgAccuracy(a.getClusterAvgAccuracy());
        info.setAssignedAt(a.getAssignedAt());
        info.setTopErrorPatterns(splitCsv(a.getClusterTopErrors()));
        return info;
    }

    /**
     * Adaptive recommendations. Prefers the cohort's top error patterns (the ML signal);
     * falls back to the student's own most frequent errors when they have no cluster yet.
     */
    private List<Recommendation> buildRecommendations(Long userId, ClusterInfo cluster) {
        List<String> sourcePatterns = (cluster.isAssigned() && !cluster.getTopErrorPatterns().isEmpty())
                ? cluster.getTopErrorPatterns()
                : ownTopErrorPatterns(userId);

        List<Recommendation> recommendations = new ArrayList<>();
        String[] severities = {"high", "medium", "low"};
        int i = 0;
        for (String pattern : sourcePatterns) {
            if (i >= 3) break;
            recommendations.add(toRecommendation(pattern, severities[i]));
            i++;
        }
        if (recommendations.isEmpty()) {
            recommendations.add(new Recommendation(
                    "Keep up the great work!",
                    "No specific weaknesses detected yet — keep practising to unlock personalised tips.",
                    null, null, null, "low"));
        }
        return recommendations;
    }

    private List<String> ownTopErrorPatterns(Long userId) {
        List<String> patterns = new ArrayList<>();
        for (ErrorPatternCountProjection e : attemptRepository.findErrorPatternCountsByUser(userId)) {
            patterns.add(e.getErrorPattern());
            if (patterns.size() >= 3) break;
        }
        return patterns;
    }

    private Recommendation toRecommendation(String errorPattern, String severity) {
        String title;
        String message;
        CalculationOperation practiceOp = null;

        switch (errorPattern == null ? "" : errorPattern) {
            case "CONFUSED_SUB_WITH_ADD":
                title = "Subtraction mix-ups";
                message = "You sometimes add instead of subtracting. A little subtraction practice will sharpen this.";
                practiceOp = CalculationOperation.SUB;
                break;
            case "MINOR_CALCULATION_ERROR":
                title = "Small calculation slips";
                message = "You're close on many answers — slow down a touch to avoid small mistakes.";
                break;
            case "EMPTY_ANSWER":
                title = "Don't leave it blank";
                message = "Some answers were left empty. Try every question — easier practice will build your confidence.";
                break;
            case "INVALID_FORMAT_ERROR":
                title = "Answer format";
                message = "A few answers weren't valid numbers. Type just the number (e.g. 12), no extra text.";
                break;
            default:
                practiceOp = operationFromGeneralError(errorPattern);
                title = "Practice opportunity";
                message = "We noticed a recurring pattern — a focused practice session will help.";
        }

        SubSubject practiceTarget = practiceOp == null ? null : resolveSubSubject(practiceOp);
        Long recommendedId = practiceTarget == null ? null : practiceTarget.getId();
        String recommendedName = practiceTarget == null ? null : practiceTarget.getName();
        return new Recommendation(title, message, errorPattern, recommendedId, recommendedName, severity);
    }

    private CalculationOperation operationFromGeneralError(String errorPattern) {
        if (errorPattern == null) return null;
        String p = errorPattern.toUpperCase();
        if (p.contains("SUBTRACTION")) return CalculationOperation.SUB;
        if (p.contains("ADDITION")) return CalculationOperation.ADD;
        if (p.contains("MULTIPLICATION")) return CalculationOperation.MULT;
        if (p.contains("DIVISION")) return CalculationOperation.DIV;
        return null;
    }

    private SubSubject resolveSubSubject(CalculationOperation op) {
        return subSubjectRepository.findByNameAndSubject_Name(op.getSubSubjectName(), CALCULATION_SUBJECT_NAME);
    }

    private List<Badge> buildBadges(OverallStats overall, int totalStars, List<TopicProgress> topics) {
        long attempts = overall.getTotalAttempts();
        long practisedTopics = topics.stream().filter(t -> t.getAttempts() > 0).count();

        List<Badge> badges = new ArrayList<>();
        badges.add(new Badge("first_steps", "First Steps", "🎯",
                "Answer your first question", attempts >= 1));
        badges.add(new Badge("sharp_shooter", "Sharp Shooter", "🏹",
                "80%+ success over at least 10 questions", attempts >= 10 && overall.getSuccessRate() >= 80));
        badges.add(new Badge("persistent", "Persistent", "💪",
                "Attempt 50 questions", attempts >= 50));
        badges.add(new Badge("star_collector", "Star Collector", "⭐",
                "Collect 100 stars", totalStars >= 100));
        badges.add(new Badge("rising_star", "Rising Star", "🚀",
                "Reach level 3 in any topic", overall.getMasteryLevel() >= 3));
        badges.add(new Badge("explorer", "Explorer", "🧭",
                "Practise 3 different topics", practisedTopics >= 3));
        return badges;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private int percent(long part, long whole) {
        return whole <= 0 ? 0 : (int) Math.round(100.0 * part / whole);
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private String displayName(User user) {
        return (user.getFullName() == null || user.getFullName().isBlank()) ? "Student" : user.getFullName();
    }
}
