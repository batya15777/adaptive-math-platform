package com.adaptive.server.service;

import com.adaptive.server.DTOs.InitialAssessmentRequest;
import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.*;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.SpaceshipStatus;
import com.adaptive.server.repository.*;
import com.adaptive.server.responses.BonusQuestionResponse;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.responses.QuestionResponse;
import com.adaptive.server.service.QuestionsGenerators.CalculationGenerator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LevelManagerService {

    // ── Adaptive-progression thresholds ──────────────────────────────────────
    static final int LEVEL_UP_WINDOW        = 30;
    static final int LEVEL_UP_THRESHOLD     = 24; // ≥ 80 %
    static final int INTERMEDIATE_WINDOW    = 10;
    static final int INTERMEDIATE_THRESHOLD =  6; // < 60 %
    static final int WEAKNESS_WINDOW        = 15;
    static final double WEAKNESS_ERROR_RATE = 0.50;
    static final int WEAKNESS_MIN_SAMPLE    =  4;
    private static final int MIN_LEVEL      =  1;

    // ── Bonus-question constants ──────────────────────────────────────────────
    private static final String CALCULATION_SUBJECT_NAME = "Calculation";
    /**
     * Maximum number of generation attempts before giving up deduplication and
     * accepting a repeated expression.  Prevents an infinite loop when the template
     * pool is smaller than the student's question history.
     */
    static final int MAX_BONUS_GEN_ATTEMPTS = 5;

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final QuestionRepository        questionRepository;
    private final QuestionArchiveRepository archiveRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final StudentProgressRepository progressRepository;
    private final UserRepository            userRepository;
    private final SubSubjectRepository      subSubjectRepository;
    private final CalculationGenerator      calculationGenerator;

    public LevelManagerService(ExerciseAttemptRepository attemptRepository,
                               StudentProgressRepository progressRepository,
                               UserRepository userRepository,
                               SubSubjectRepository subSubjectRepository,
                               CalculationGenerator calculationGenerator,
                               QuestionRepository questionRepository,
                               QuestionArchiveRepository archiveRepository) {
        this.attemptRepository    = attemptRepository;
        this.progressRepository   = progressRepository;
        this.userRepository       = userRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.calculationGenerator = calculationGenerator;
        this.questionRepository   = questionRepository;
        this.archiveRepository    = archiveRepository;
    }


    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Records the student's answer, re-evaluates their level, and returns the
     * updated progress status.
     *
     * <p>When the student levels up (BOOSTING), {@code bonusQuestionTriggered=true}
     * is set on the response so the frontend immediately opens the bonus-question flow.
     */
    @Transactional
    public ProgressStatusResponse submitAnswer(Long userId, SubmitAnswerRequest request) {
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(request.getSubSubjectId());

        // 1. שליפת השאלה מה-DB כדי לבדוק את התשובה בשרת
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // השרת מחליט אם התשובה נכונה (מניעת רמאויות מצד הלקוח)
        boolean isCorrect = question.getCorrectAnswer().equals(request.getUserAnswer());

        // 2. שמירת הניסיון ב-DB עם התוצאה שהשרת חישב
        saveAttempt(user, subSubject, request, isCorrect);

        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, request.getSubSubjectId());
        List<ExerciseAttempt> last30 = fetchRecent(userId, request.getSubSubjectId(), LEVEL_UP_WINDOW);

        long correct30 = countCorrect(last30);
        long correct10 = countCorrect(slice(last30, INTERMEDIATE_WINDOW));

        SpaceshipStatus status = evaluateSpaceshipStatus(total, correct30, correct10);
        applyStatusToProgress(progress, status);
        progressRepository.save(progress);

        String weakness = detectWeakness(slice(last30, WEAKNESS_WINDOW));

        // 3. החזרת ה-Response עם התוצאה המחושבת של השרת
        return buildResponse(user, progress, status, correct10, correct30, total, weakness, isCorrect);
    }
    /**
     * Returns the student's current progress for a sub-subject without recording any attempt.
     */
    @Transactional(readOnly = true)
    public ProgressStatusResponse getStatus(Long userId, Long subSubjectId) {
        return progressRepository
                .findByUserIdAndSubSubjectId(userId, subSubjectId)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(progress -> buildStatusFromHistory(progress, userId, subSubjectId))
                .orElseGet(this::buildDefaultStatus);
    }

    /**
     * Generates the next adaptive practice question and archives it immediately so
     * the student's history is always up to date.
     *
     * <p>Template selection now uses the <strong>3-parameter query</strong>
     * ({@code subSubject + difficultyLevel + subSubjectLevel}) via
     * {@code CalculationGenerator}, which clamps the student's level to the highest
     * seeded band ({@code MAX_TEMPLATE_LEVEL = 3}) internally.
     */
    @Transactional
    public QuestionResponse getNextQuestion(Long userId, Long subSubjectId,
                                            String language, boolean multipleChoice) {
        User user             = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);

        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        int difficultyLevel      = progress.getCurrentLevel();
        int subSubjectLevel      = progress.getCurrentLevel();

        List<ExerciseAttempt> last15 = fetchRecent(userId, subSubjectId, WEAKNESS_WINDOW);
        String weakness              = detectWeakness(last15);

        SubSubject targetSubSubject = resolveTargetSubSubject(subSubject, weakness);

        Question question = calculationGenerator.createQuestion(
                targetSubSubject, subSubjectLevel, difficultyLevel, language, multipleChoice);
        question = questionRepository.save(question);

        // Archive so this question counts toward the student's history
        archiveQuestion(user, targetSubSubject, question);

        return buildQuestionResponse(question, subSubjectId, weakness);
    }

    /**
     * Generates an initial assessment question for a newly registered user based on their
     * school grade and confidence level.
     *
     * Grade Mapping:
     * 1-3 -> Level 1
     * 4-6 -> Level 2
     * 7+  -> Level 3
     *
     * Confidence Mapping:
     * EASY -> Level 1
     * MEDIUM -> Level 2
     * HARD -> Level 3
     */
    @Transactional
    public QuestionResponse getInitialAssessmentQuestion(Long userId, InitialAssessmentRequest request) {
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(request.getSubSubjectId());

        int subSubjectLevel = mapGradeToLevel(request.getGrade());
        int difficultyLevel = mapConfidenceToLevel(request.getConfidenceLevel());

        // We can create or update the student's progress to start at this calculated level
        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        progress.setCurrentLevel(subSubjectLevel);
        progress.setCurrentProgress(0);
        progressRepository.save(progress);

        String language = request.getLanguage() != null ? request.getLanguage() : "he";

        Question question = calculationGenerator.createQuestion(
                subSubject, subSubjectLevel, difficultyLevel, language, true);
        question = questionRepository.save(question);

        archiveQuestion(user, subSubject, question);

        return buildQuestionResponse(question, subSubject.getId(), null);
    }

    private int mapGradeToLevel(Integer grade) {
        if (grade == null || grade <= 3) return 1;
        if (grade <= 6) return 2;
        return 3;
    }

    private int mapConfidenceToLevel(String confidence) {
        if (confidence == null) return 2;
        switch (confidence.toUpperCase()) {
            case "EASY": return 1;
            case "HARD": return 3;
            case "MEDIUM":
            default: return 2;
        }
    }

    /**
     * Generates a Bonus Question awarded when the student levels up.
     *
     * <h3>Deduplication</h3>
     * Before generating, the method fetches the full set of expression strings the
     * student has previously seen for the "mixed" sub-subject from {@code question_archive}.
     * It retries generation up to {@value #MAX_BONUS_GEN_ATTEMPTS} times to find a
     * non-duplicate.  If every attempt produces a repeated expression (e.g. the template
     * pool is exhausted), the last candidate is used rather than blocking indefinitely.
     *
     * @param userId       authenticated user
     * @param subSubjectId the sub-subject the student was practising when they levelled up
     * @param language     preferred question language
     */
    @Transactional
    public BonusQuestionResponse getBonusQuestion(Long userId, Long subSubjectId, String language) {
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);
        StudentProgress progress = loadOrCreateProgress(user, subSubject);

        int currentLevel    = progress.getCurrentLevel();
        int bonusDifficulty = calculationGenerator.getMaxDifficultyLevelForSubSubject(subSubject);

        // ── Deduplication: fetch expressions already shown to this user ───────
        Set<String> seenExpressions = archiveRepository
                .findSeenExpressionsByUserAndSubSubject(userId, subSubject.getId());

        // ── Generate, retrying until a fresh expression is found ──────────────
        Question question = null;
        for (int attempt = 0; attempt < MAX_BONUS_GEN_ATTEMPTS; attempt++) {
            Question candidate = calculationGenerator.createQuestion(
                    subSubject, currentLevel, bonusDifficulty, language, true);
            if (!seenExpressions.contains(candidate.getExpression())) {
                question = candidate;
                break;
            }
        }
        // Pool exhausted — accept the last candidate rather than blocking
        if (question == null) {
            question = calculationGenerator.createQuestion(
                    subSubject, currentLevel, bonusDifficulty, language, true);
        }

        question = questionRepository.save(question);
        // Archive so the next bonus call won't show the same expression again
        archiveQuestion(user, subSubject, question);

        return buildBonusQuestionResponse(question, subSubjectId);
    }

    /**
     * Records the outcome of a Bonus Question.
     *
     * <p>If {@code correct == true}, exactly {@value BonusQuestionResponse#BONUS_STARS} stars
     * are added to {@link User#getTotalStars()} and persisted in the same transaction.
     */
    @Transactional
    public ProgressStatusResponse submitBonusAnswer(Long userId, Long subSubjectId, boolean correct) {
        User user = resolveUser(userId);

        if (correct) {
            user.setTotalStars(user.getTotalStars() + BonusQuestionResponse.BONUS_STARS);
            userRepository.save(user);
        }

        return progressRepository
                .findByUserIdAndSubSubjectId(userId, subSubjectId)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(progress -> buildStatusFromHistory(progress, userId, subSubjectId, user))
                .orElseGet(() -> buildDefaultStatus(user));
    }


    // ── Level-evaluation logic (package-private for unit tests) ───────────────

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


    // ── Private helpers ───────────────────────────────────────────────────────

    private SubSubject resolveTargetSubSubject(SubSubject subSubject, String weakness) {
        if (weakness != null) {
            CalculationOperation fromWeakness = questionTypeToOperation(weakness);
            if (fromWeakness != null) {
                SubSubject weaknessSubSubject = subSubjectRepository
                        .findByNameAndSubject_Name(fromWeakness.getSubSubjectName(),
                                CALCULATION_SUBJECT_NAME);
                if (weaknessSubSubject != null) return weaknessSubSubject;
            }
        }
        return subSubject;
    }

    private CalculationOperation questionTypeToOperation(String questionType) {
        if (questionType == null) return null;
        switch (questionType.toUpperCase()) {
            case "SIMPLE_ADDITION":
            case "CARRYING_ADDITION":    return CalculationOperation.ADD;
            case "SIMPLE_SUBTRACTION":
            case "CARRYING_SUBTRACTION": return CalculationOperation.SUB;
            case "MULTIPLICATION":       return CalculationOperation.MULT;
            case "DIVISION":             return CalculationOperation.DIV;
            default:                     return null;
        }
    }

    /**
     * Saves a snapshot of the generated question into {@code question_archive}.
     *
     * <p>This is called immediately after a question is shown to the student (in both
     * {@code getNextQuestion} and {@code getBonusQuestion}).  The archive record is a
     * denormalized copy of the question data — it does not reference the {@code questions}
     * table — so the history survives even if the live question is later cleaned up.
     */
    private void archiveQuestion(User user, SubSubject subSubject, Question question) {
        // 1. יצירת עותקים בטוחים של הרשימות (מניעת Shared References)
        List<String> solutionCopy = (question.getSolution() != null)
                ? new ArrayList<>(question.getSolution())
                : new ArrayList<>();

        List<String> optionsCopy = (question.getOptions() != null)
                ? new ArrayList<>(question.getOptions())
                : new ArrayList<>();

        // 2. בניית האובייקט עם הרשימות עצמן
        QuestionArchive archive = new QuestionArchive(
                user,
                subSubject,
                question.getExpression(),
                question.getCorrectAnswer(),
                solutionCopy, // מעבירים List
                optionsCopy,  // מעבירים List
                question.getLanguage(),
                question.getDifficultyLevel()
        );

        archiveRepository.save(archive);
    }

    private ProgressStatusResponse buildResponse(User user, StudentProgress progress,
                                                 SpaceshipStatus status,
                                                 long correct10, long correct30, long total,
                                                 String weakness, boolean lastAnswerCorrect) {
        boolean isLevelUp      = (status == SpaceshipStatus.BOOSTING);
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
        response.setTotalStars(user.getTotalStars());
        response.setBonusQuestionTriggered(isLevelUp);
        response.setMessage(resolveMessage(status, weakness, lastAnswerCorrect));
        return response;
    }

    private ProgressStatusResponse buildStatusFromHistory(StudentProgress progress,
                                                          Long userId, Long subSubjectId) {
        User user = resolveUser(userId);
        return buildStatusFromHistory(progress, userId, subSubjectId, user);
    }

    private ProgressStatusResponse buildStatusFromHistory(StudentProgress progress,
                                                          Long userId, Long subSubjectId,
                                                          User user) {
        long total   = attemptRepository.countByUserIdAndSubSubjectId(userId, subSubjectId);
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
        r.setTotalStars(user.getTotalStars());
        r.setMessage("המשך לתרגל — אתה עושה עבודה מצוינת! ⭐");
        return r;
    }

    private ProgressStatusResponse buildDefaultStatus() {
        return buildDefaultStatus(null);
    }

    private ProgressStatusResponse buildDefaultStatus(User user) {
        ProgressStatusResponse r = new ProgressStatusResponse();
        r.setSuccess(true);
        r.setCurrentLevel(MIN_LEVEL);
        r.setSpaceshipStatus(SpaceshipStatus.MOVING.name());
        r.setCorrectLast10(0);
        r.setCorrectLast30(0);
        r.setTotalAttempts(0);
        r.setTotalStars(user != null ? user.getTotalStars() : 0);
        r.setMessage("עוד לא התחלת נושא זה. בוא נתחיל! 🚀");
        return r;
    }

    /**
     * Maps a generated {@link Question} onto the wire-format {@link QuestionResponse}.
     * {@code Question.getSolution()} is a {@code List<String>} of step strings;
     * we join with {@code "\n"} so the frontend can split and display them line-by-line.
     */
    private QuestionResponse buildQuestionResponse(Question question, Long subSubjectId,
                                                   String weakness) {
        QuestionResponse response = new QuestionResponse();
        populateQuestionFields(response, question, subSubjectId);
        response.setRecommendedQuestionType(weakness);
        response.setMessage(weakness != null
                ? "נחש! אתה מתאמן על: " + formatQuestionType(weakness)
                : "שאלה חדשה מחכה לך! 💪");
        return response;
    }

    private BonusQuestionResponse buildBonusQuestionResponse(Question question, Long subSubjectId) {
        BonusQuestionResponse response = new BonusQuestionResponse();
        populateQuestionFields(response, question, subSubjectId);
        response.setMessage("🌟 שאלת בונוס! ענה נכון וקבל " + BonusQuestionResponse.BONUS_STARS + " כוכבים!");
        return response;
    }

    private void populateQuestionFields(QuestionResponse response, Question question,
                                        Long subSubjectId) {
        String solutionText = question.getSolution() == null
                ? ""
                : question.getSolution().stream().collect(Collectors.joining("\n"));

        response.setSuccess(true);
        response.setQuestionId(question.getId());
        response.setExpression(question.getExpression());
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setSolution(solutionText);
        response.setOptions(question.getOptions() == null ? null
                : String.join(",", question.getOptions()));
        response.setDifficultyLevel(question.getDifficultyLevel());
        response.setSubSubjectId(subSubjectId);
    }

    private String resolveMessage(SpaceshipStatus status, String weakness, boolean lastCorrect) {
        switch (status) {
            case BOOSTING:
                return "כל הכבוד! עלית רמה! 🚀 מחכה לך שאלת בונוס — 50 כוכבים על הפרק!";
            case STOPPED:
                return weakness != null
                        ? "אתה מתקשה ב-" + formatQuestionType(weakness) + " – בוא נתרגל אותו יותר! 🎯"
                        : "בוא נתרגל קצת יותר לאט — אתה כמעט שם! 💪";
            default:
                return lastCorrect ? "מצוין! תשובה נכונה! ✅" : "לא נורא, נסה שוב! 💡";
        }
    }

    private void saveAttempt(User user, SubSubject subSubject, SubmitAnswerRequest req, boolean isCorrect) {
        ExerciseAttempt attempt = new ExerciseAttempt(
                user,
                subSubject,
                req.getQuestionId(),      // 3
                isCorrect,                // 4 - הוספנו את התוצאה שחישבנו!
                req.getCurrentDifficulty(), // 5
                req.getQuestionType(),    // 6
                LocalDateTime.now()       // 7
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
