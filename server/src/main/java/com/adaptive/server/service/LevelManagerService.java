package com.adaptive.server.service;

import com.adaptive.server.DTOs.ProgressStatusResponse;
import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.ExerciseAttempt;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.SpaceshipStatus;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.StudentProgressRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LevelManagerService {
    private static final int LEVEL_UP_WINDOW = 30;
    private static final int LEVEL_UP_THRESHOLD = 24;
    private static final int INTERMEDIATE_WINDOW = 10;
    private static final int INTERMEDIATE_THRESHOLD = 6;
    private static final int WEAKNESS_WINDOW = 15;
    private static final double WEAKNESS_ERROR_RATE = 0.50;
    private static final int WEAKNESS_MIN_SAMPLE = 4;

    private final ExerciseAttemptRepository attemptRepository;
    private final StudentProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final SubSubjectRepository subSubjectRepository;

    public LevelManagerService(ExerciseAttemptRepository attemptRepository,
                               StudentProgressRepository progressRepository,
                               UserRepository userRepository,
                               SubSubjectRepository subSubjectRepository) {
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.subSubjectRepository = subSubjectRepository;
    }

    /**
     * @param userId  Authenticated user ID (extracted from session token by controller).
     * @param request Answer details from the frontend.
     */
    @Transactional
    public ProgressStatusResponse submitAnswer(Long userId, SubmitAnswerRequest request) {
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(request.getSubSubjectId());

        saveAttempt(user, subSubject, request);

        StudentProgress progress = loadOrCreateProgress(user, subSubject);

        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, request.getSubSubjectId());
        List<ExerciseAttempt> last30 = fetchRecent(userId, request.getSubSubjectId(), LEVEL_UP_WINDOW);

        long correct30 = countCorrect(last30);
        long correct10 = countCorrect(slice(last30, INTERMEDIATE_WINDOW));

        SpaceshipStatus status = evaluateSpaceshipStatus(total, correct30, correct10);
        applyStatusToProgress(progress, status);
        progressRepository.save(progress);

        String weakness = detectWeakness(slice(last30, WEAKNESS_WINDOW));

        return buildResponse(progress, status, correct10, correct30, total, weakness, request.isCorrect());
    }

    /**
     * @param userId Authenticated user ID (extracted from session token by controller).
     * @param subSubjectId The sub-subject to query.
     */
    @Transactional(readOnly = true)
    public ProgressStatusResponse getStatus(Long userId, Long subSubjectId) {

        return progressRepository
                .findByUserIdAndSubSubjectId(userId, subSubjectId)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(progress -> buildStatusFromHistory(progress, userId, subSubjectId))
                .orElseGet(this::buildDefaultStatus);
    }

    private SpaceshipStatus evaluateSpaceshipStatus(long total, long correct30, long correct10) {
        if (total >= LEVEL_UP_WINDOW && correct30 >= LEVEL_UP_THRESHOLD) {
            return SpaceshipStatus.BOOSTING;
        }
        if (total >= INTERMEDIATE_WINDOW && correct10 < INTERMEDIATE_THRESHOLD) {
            return SpaceshipStatus.STOPPED;
        }
        return SpaceshipStatus.MOVING;
    }


    private void applyStatusToProgress(StudentProgress progress, SpaceshipStatus status) {
        switch (status) {
            case BOOSTING:
                progress.setCurrentLevel(progress.getCurrentLevel() + 1);
                progress.setCurrentProgress(0);
                break;
            case MOVING:
                int current = progress.getCurrentProgress() == null ? 0 : progress.getCurrentProgress();
                progress.setCurrentProgress(current + 1);
                break;
            case STOPPED:
            default:
                // Spaceship stays in place - no progress changes
                break;
        }
    }


    private String detectWeakness(List<ExerciseAttempt> attempts) {
        if (attempts.isEmpty()) return null;

        Map<String, TypeStats> stats = new HashMap<>();

        for (ExerciseAttempt attempt : attempts) {
            String type = attempt.getQuestionType();
            if (type == null) continue;
            stats.computeIfAbsent(type, k -> new TypeStats())
                 .record(Boolean.FALSE.equals(attempt.getIsCorrect()));
        }

        return stats.entrySet().stream()
                .filter(e -> e.getValue().total >= WEAKNESS_MIN_SAMPLE)
                .filter(e -> e.getValue().errorRate() > WEAKNESS_ERROR_RATE)
                .max(Comparator.comparingDouble(e -> e.getValue().errorRate()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    private ProgressStatusResponse buildResponse(StudentProgress progress,
                                                  SpaceshipStatus status,
                                                  long correct10, long correct30, long total,
                                                  String weakness, boolean lastAnswerCorrect) {
        boolean isLevelUp = (status == SpaceshipStatus.BOOSTING);
        boolean isIntermediate = (status == SpaceshipStatus.STOPPED);

        ProgressStatusResponse response = new ProgressStatusResponse();
        response.setSuccess(true);
        response.setCurrentLevel(progress.getCurrentLevel());
        response.setSpaceshipStatus(status.name());
        response.setLevelUp(isLevelUp);
        response.setInIntermediateLevel(isIntermediate);
        response.setWeaknessType(weakness);
        response.setRecommendedQuestionType(isIntermediate ? weakness : null);
        response.setCorrectLast10((int) correct10);
        response.setCorrectLast30((int) correct30);
        response.setTotalAttempts(total);
        response.setMessage(resolveMessage(status, weakness, lastAnswerCorrect));
        return response;
    }

    private ProgressStatusResponse buildStatusFromHistory(StudentProgress progress, Long userId, Long subSubjectId) {
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, subSubjectId);
        List<ExerciseAttempt> last30 = fetchRecent(userId, subSubjectId, LEVEL_UP_WINDOW);

        long correct30 = countCorrect(last30);
        long correct10 = countCorrect(slice(last30, INTERMEDIATE_WINDOW));
        SpaceshipStatus status = evaluateSpaceshipStatus(total, correct30, correct10);
        String weakness = detectWeakness(slice(last30, WEAKNESS_WINDOW));

        ProgressStatusResponse r = new ProgressStatusResponse();
        r.setSuccess(true);
        r.setCurrentLevel(progress.getCurrentLevel());
        r.setSpaceshipStatus(status.name());
        r.setWeaknessType(weakness);
        r.setCorrectLast10((int) correct10);
        r.setCorrectLast30((int) correct30);
        r.setTotalAttempts(total);
        r.setMessage("המשך לתרגל — אתה עושה עבודה מצוינת! ⭐");
        return r;
    }

    private ProgressStatusResponse buildDefaultStatus() {
        ProgressStatusResponse r = new ProgressStatusResponse();
        r.setSuccess(true);
        r.setCurrentLevel(1);
        r.setSpaceshipStatus(SpaceshipStatus.MOVING.name());
        r.setCorrectLast10(0);
        r.setCorrectLast30(0);
        r.setTotalAttempts(0);
        r.setMessage("עוד לא התחלת נושא זה. בוא נתחיל! 🚀");
        return r;
    }


    private String resolveMessage(SpaceshipStatus status, String weakness, boolean lastCorrect) {
        switch (status) {
            case BOOSTING:
                return "כל הכבוד! עלית רמה! 🚀 המשך כך!";
            case STOPPED:
                return weakness != null
                        ? "אתה מתקשה ב-" + formatQuestionType(weakness) + " – בוא נתרגל אותו יותר! 🎯"
                        : "בוא נתרגל קצת יותר לאט — אתה כמעט שם! 💪";
            default:
                return lastCorrect ? "מצוין! תשובה נכונה! ✅" : "לא נורא, נסה שוב! 💡";
        }
    }


    private void saveAttempt(User user, SubSubject subSubject, SubmitAnswerRequest req) {
        ExerciseAttempt attempt = new ExerciseAttempt(
                user, subSubject,
                req.getQuestionId(),
                req.isCorrect(),
                req.getCurrentDifficulty(),
                req.getQuestionType(),
                LocalDateTime.now()
        );
        attemptRepository.save(attempt);
    }

    private StudentProgress loadOrCreateProgress(User user, SubSubject subSubject) {
        return progressRepository
                .findByUserIdAndSubSubjectId(user.getId(), subSubject.getId())
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .orElseGet(() -> createNewProgress(user, subSubject));
    }

    private StudentProgress createNewProgress(User user, SubSubject subSubject) {
        return progressRepository.save(new StudentProgress(user, subSubject, 1, 0, true));
    }

    private List<ExerciseAttempt> fetchRecent(Long userId, Long subSubjectId, int limit) {
        return attemptRepository.findByUserIdAndSubSubjectId(
                userId, subSubjectId,
                PageRequest.of(0, limit, Sort.by("answeredAt").descending()));
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private SubSubject resolveSubSubject(Long subSubjectId) {
        return subSubjectRepository.findById(subSubjectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SubSubject not found: " + subSubjectId));
    }


    private long countCorrect(List<ExerciseAttempt> attempts) {
        return attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .count();
    }

    private List<ExerciseAttempt> slice(List<ExerciseAttempt> list, int max) {
        return list.size() > max ? list.subList(0, max) : list;
    }

    private String formatQuestionType(String questionType) {
        switch (questionType) {
            case "SIMPLE_ADDITION":      return "חיבור פשוט";
            case "CARRYING_ADDITION":    return "חיבור עם העברה";
            case "SIMPLE_SUBTRACTION":   return "חיסור פשוט";
            case "CARRYING_SUBTRACTION": return "חיסור עם העברה";
            case "WORD_PROBLEM":         return "שאלות מילוליות";
            case "MISSING_NUMBER":       return "מספר חסר";
            case "MULTIPLE_CHOICE":      return "בחירה מרובה";
            default:                     return questionType;
        }
    }


    private static class TypeStats {
        int total;
        int wrong;

        void record(boolean isWrong) {
            total++;
            if (isWrong) wrong++;
        }

        double errorRate() {
            return total == 0 ? 0.0 : (double) wrong / total;
        }
    }
}
