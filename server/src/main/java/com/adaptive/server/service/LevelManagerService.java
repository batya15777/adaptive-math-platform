package com.adaptive.server.service;

import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.ExerciseAttempt;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.SpaceshipStatus;
import com.adaptive.server.repository.*;
import com.adaptive.server.service.QuestionsGenerators.CalculationGenerator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.responses.QuestionResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LevelManagerService {
    static final int LEVEL_UP_WINDOW = 30;
    static final int LEVEL_UP_THRESHOLD = 24; // ≥ 80 %
    static final int INTERMEDIATE_WINDOW = 10;
    static final int INTERMEDIATE_THRESHOLD = 6; // < 60 %
    static final int WEAKNESS_WINDOW = 15;
    static final double WEAKNESS_ERROR_RATE = 0.50;
    static final int WEAKNESS_MIN_SAMPLE = 4;
    private static final int MIN_LEVEL = 1;


    private final QuestionRepository questionRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final StudentProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final CalculationGenerator calculationGenerator;

    public LevelManagerService(ExerciseAttemptRepository attemptRepository, StudentProgressRepository progressRepository,
                               UserRepository userRepository, SubSubjectRepository subSubjectRepository,
                               CalculationGenerator calculationGenerator , QuestionRepository questionRepository) {
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.calculationGenerator = calculationGenerator;
        this.questionRepository = questionRepository;
    }


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


    @Transactional(readOnly = true)
    public ProgressStatusResponse getStatus(Long userId, Long subSubjectId) {
        return progressRepository
                .findByUserIdAndSubSubjectId(userId, subSubjectId)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(progress -> buildStatusFromHistory(progress, userId, subSubjectId))
                .orElseGet(this::buildDefaultStatus);
    }


    @Transactional
    public QuestionResponse getNextQuestion(Long userId, Long subSubjectId, String language, boolean multipleChoice) {
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);
        // Load or create progress to read the current difficulty level
        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        int difficultyLevel = progress.getCurrentLevel();
        // Detect weakness to guide operation selection
        List<ExerciseAttempt> last15 = fetchRecent(userId, subSubjectId, WEAKNESS_WINDOW);
        String weakness = detectWeakness(last15);
        // Pick which CalculationOperation to generate; weakness overrides random selection
        CalculationOperation operation = resolveOperation(subSubject, weakness);
        // Delegate to the teammate's generator — this is the integration point
        Question question = calculationGenerator.createQuestion(operation, difficultyLevel, language, multipleChoice);
        question = questionRepository.save(question); //שמירת שאלה לתוך DB
        return buildQuestionResponse(question, subSubjectId, weakness);
    }


    SpaceshipStatus evaluateSpaceshipStatus(long total, long correct30, long correct10) {
        if (total >= LEVEL_UP_WINDOW && correct30 >= LEVEL_UP_THRESHOLD) {
            return SpaceshipStatus.BOOSTING;
        }
        if (total >= INTERMEDIATE_WINDOW && correct10 < INTERMEDIATE_THRESHOLD) {
            return SpaceshipStatus.STOPPED;
        }
        return SpaceshipStatus.MOVING;
    }


    void applyStatusToProgress(StudentProgress progress, SpaceshipStatus status) {
        switch (status) {
            case BOOSTING:
                progress.setCurrentLevel(progress.getCurrentLevel() + 1);
                progress.setCurrentProgress(0);
                break;

            case STOPPED:
                // Drop to an intermediate easier level (never below MIN_LEVEL)
                progress.setCurrentLevel(Math.max(MIN_LEVEL, progress.getCurrentLevel() - 1));
                progress.setCurrentProgress(0);
                break;

            case MOVING:
            default:
                int current = progress.getCurrentProgress() == null ? 0 : progress.getCurrentProgress();
                progress.setCurrentProgress(current + 1);
                break;
        }
    }

   //זיהוי חולשות
    String detectWeakness(List<ExerciseAttempt> attempts) {
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

    private CalculationOperation resolveOperation(SubSubject subSubject, String weakness) {
        if (weakness != null) {
            CalculationOperation fromWeakness = questionTypeToOperation(weakness);
            if (fromWeakness != null) {
                return fromWeakness;
            }
        }
        try {
            return CalculationOperation.from(subSubject.getName());
        } catch (IllegalArgumentException e) {
            return CalculationOperation.ADD; // Safe fallback
        }
    }


    private CalculationOperation questionTypeToOperation(String questionType) {
        if (questionType == null) return null;
        switch (questionType.toUpperCase()) {
            case "SIMPLE_ADDITION":
            case "CARRYING_ADDITION":
                return CalculationOperation.ADD;
            case "SIMPLE_SUBTRACTION":
            case "CARRYING_SUBTRACTION":
                return CalculationOperation.SUB;
            case "MULTIPLICATION":
                return CalculationOperation.MULT;
            case "DIVISION":
                return CalculationOperation.DIV;
            default:
                return null;
        }
    }


    private ProgressStatusResponse buildResponse(StudentProgress progress, SpaceshipStatus status,
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
        SpaceshipStatus status  = evaluateSpaceshipStatus(total, correct30, correct10);
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
        r.setCurrentLevel(MIN_LEVEL);
        r.setSpaceshipStatus(SpaceshipStatus.MOVING.name());
        r.setCorrectLast10(0);
        r.setCorrectLast30(0);
        r.setTotalAttempts(0);
        r.setMessage("עוד לא התחלת נושא זה. בוא נתחיל! 🚀");
        return r;
    }

    private QuestionResponse buildQuestionResponse(Question question, Long subSubjectId, String weakness) {
        QuestionResponse response = new QuestionResponse();
        response.setSuccess(true);
        response.setQuestionId(question.getId());  // may be null if not yet persisted
        response.setExpression(question.getExpression());
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setSolution(question.getSolution());
        response.setOptions(question.getOptions());
        response.setDifficultyLevel(question.getDifficultyLevel());
        response.setSubSubjectId(subSubjectId);
        response.setRecommendedQuestionType(weakness);
        response.setMessage(weakness != null
                ? "נחש! אתה מתאמן על: " + formatQuestionType(weakness)
                : "שאלה חדשה מחכה לך! 💪");
        return response;
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
        return progressRepository.save(new StudentProgress(user, subSubject, MIN_LEVEL, 0, true));
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
            case "MULTIPLICATION":       return "כפל";
            case "DIVISION":             return "חילוק";
            default:                     return questionType;
        }
    }

    static class TypeStats {
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
