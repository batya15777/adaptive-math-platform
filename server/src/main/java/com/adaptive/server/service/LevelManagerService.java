package com.adaptive.server.service;

import com.adaptive.server.DTOs.InitialAssessmentRequest;
import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.*;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.entity.enums.SpaceshipStatus;
import com.adaptive.server.repository.*;
import com.adaptive.server.responses.BonusQuestionResponse;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.responses.QuestionResponse;
import com.adaptive.server.service.QuestionsGenerators.CalculationGenerator;
import com.adaptive.server.service.QuestionsGenerators.PolynomialGenerator;
import com.adaptive.server.service.QuestionsGenerators.ClusterContext;
import com.adaptive.server.service.errorpattern.ErrorPatternService;
import com.adaptive.server.service.sse.AdminSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LevelManagerService {//מוח שמנהל התקדמות תלמיד מחלקה חשובה

    static final int LEVEL_UP_WINDOW = 16;
    static final int LEVEL_UP_THRESHOLD = 12; // ≥ 80 %
    static final int INTERMEDIATE_WINDOW = 10;
    static final int INTERMEDIATE_THRESHOLD = 6;
    static final int WEAKNESS_MIN_SAMPLE = 4;
    private static final int MIN_LEVEL = 1;//רמה מינימלית שלא נרד לרמה 0

    // ── question-game progression ────────────────────────────────────────
    static final int MAX_ATTEMPTS_PER_QUESTION = 3;  // 3rd wrong → FAILED + reveal
    static final int SUB_LEVEL_ENTER_FAILS = 3;      // FAILED in a row → enter sub-level
    static final int SUB_LEVEL_EXIT_SOLVES = 3;      // SOLVED in sub-level → leave it
    static final int MC_MAX_DIFFICULTY = 2;          // difficulty ≤ 2 → multiple choice
    static final int MAX_DIFFICULTY_BAND = 3;        // structural template band cap (seeded 1–3)
    static final int MAX_DIFFICULTY_LEVEL = 10;      // difficulty scale upper bound (1–10)
    static final int MAX_DEDUP_ATTEMPTS = 3;         // regen tries to avoid a seen expression
    static final int STARS_NORMAL = 10;              // correct at normal level
    static final int STARS_SUBLEVEL = 5;             // correct inside the sub-level

    static final int MAX_BONUS_GEN_ATTEMPTS = 5;//המערכת תנסה להגריל שאלה חדשה מקסימום 5 פעמים זה מונע לולאה אינסופית

    private static final Logger log = LoggerFactory.getLogger(LevelManagerService.class);
    private static final int AI_MIN_DIFFICULTY = 1;
    private static final int AI_MAX_DIFFICULTY = 10;

    private final QuestionRepository questionRepository;
    private final QuestionArchiveRepository archiveRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final StudentProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final SubSubjectAiConfigRepository subSubjectAiConfigRepository;
    private final CalculationGenerator calculationGenerator;
    private final PolynomialGenerator polynomialGenerator;
    private final ClusterContextService clusterContextService;
    private final AiQuestionService aiQuestionService;
    private final ErrorPatternService errorPatternService;
    private final AdminSseService adminSseService;

    // Coin flip for the multiple-choice vs typed-answer decision at difficulty ≥ 3.
    private final Random random = new Random();

    // When true, the live next-question flow tries the AI generator first (with a safe
    // fallback to the code generator). Off by default so behaviour is unchanged until enabled.
    @Value("${questions.ai.enabled:false}")
    private boolean aiQuestionsEnabled;

    @Value("${questions.ai.theme:space}")
    private String aiQuestionTheme;

    public LevelManagerService(ExerciseAttemptRepository attemptRepository, StudentProgressRepository progressRepository,
                               UserRepository userRepository, SubSubjectRepository subSubjectRepository,
                               SubSubjectAiConfigRepository subSubjectAiConfigRepository,
                               CalculationGenerator calculationGenerator, PolynomialGenerator polynomialGenerator,
                               QuestionRepository questionRepository,
                               QuestionArchiveRepository archiveRepository,
                               ClusterContextService clusterContextService, AiQuestionService aiQuestionService,
                               ErrorPatternService errorPatternService,
                               AdminSseService adminSseService) {
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.subSubjectAiConfigRepository = subSubjectAiConfigRepository;
        this.calculationGenerator = calculationGenerator;
        this.polynomialGenerator = polynomialGenerator;
        this.questionRepository = questionRepository;
        this.archiveRepository = archiveRepository;
        this.clusterContextService = clusterContextService;
        this.aiQuestionService = aiQuestionService;
        this.errorPatternService = errorPatternService;
        this.adminSseService = adminSseService;
    }


    @Transactional
    public ProgressStatusResponse submitAnswer(Long userId, SubmitAnswerRequest request) {
        // Counter/outcome model: a question allows up to 3 attempts. Correct → SOLVED,
        // 3rd wrong → FAILED. SOLVED advances the level-up window (and stars); a run of
        // FAILED questions drops the student into the easier "sub-level".
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(request.getSubSubjectId());

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        boolean isCorrect = question.getCorrectAnswer().equals(request.getUserAnswer());
        String errorPattern = !isCorrect
                ? errorPatternService.analyze(subSubject, question.getExpression(), question.getCorrectAnswer(),
                                      request.getUserAnswer(), request.getQuestionType())
                : null;
        saveAttempt(user, subSubject, request.getQuestionId(), isCorrect, request.getCurrentDifficulty(),
                request.getQuestionType(), request.getUserAnswer(), errorPattern, LocalDateTime.now());
        adminSseService.pushAnalyticsUpdate();

        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, request.getSubSubjectId());

        boolean levelUp = false;
        String concluded = null;
        // Daily Practice grants ONLY the +100 completion bonus (claimed separately), never the
        // per-question reward — so we suppress the per-answer stars here. Regular Practice sends
        // no flag (defaults false) and keeps its normal +10 / +5 reward untouched.
        boolean awardStars = !request.isDailyPractice();

        if (isCorrect) {
            question.setStatus(QuestionStatus.SOLVED);
            concluded = "SOLVED";
            progress.setCurrentStreak(progress.getCurrentStreak() + 1);
            if (progress.getInSubLevel()) {
                // Sub-level: easier practice. Stars but NO level-progress change (bar frozen).
                if (awardStars) user.setTotalStars(user.getTotalStars() + STARS_SUBLEVEL);
                progress.setSubLevelProgress(progress.getSubLevelProgress() + 1);
                if (progress.getSubLevelProgress() >= SUB_LEVEL_EXIT_SOLVES) {
                    // recovered — return to the normal level; level progress is preserved (not reset)
                    progress.setInSubLevel(false);
                    progress.setSubLevelProgress(0);
                    progress.setConsecutiveFails(0);
                }
            } else {
                // Normal level: advance the level-progress counter; level up when it hits the target.
                if (awardStars) user.setTotalStars(user.getTotalStars() + STARS_NORMAL);
                progress.setConsecutiveFails(0);
                int newProgress = currentProgressOf(progress) + 1;
                if (newProgress >= LEVEL_UP_THRESHOLD) {
                    progress.setCurrentLevel(progress.getCurrentLevel() + 1);
                    progress.setCurrentProgress(0);
                    levelUp = true;
                } else {
                    progress.setCurrentProgress(newProgress);
                }
            }
            userRepository.save(user);
        } else {
            // Any wrong answer — reset streak immediately
            progress.setCurrentStreak(0);
            if (request.getAttemptNumber() >= MAX_ATTEMPTS_PER_QUESTION) {
                question.setStatus(QuestionStatus.FAILED);
                concluded = "FAILED";
                onFailed(progress);
            }
        }
        // wrong with attempts left → not concluded; the client lets the student retry

        if (concluded != null) {
            progress.setActiveQuestionId(null);
        }
        questionRepository.save(question);
        progressRepository.save(progress);

        ProgressStatusResponse response = buildGameResponse(user, progress, total);
        response.setAnswerCorrect(isCorrect);
        response.setConcluded(concluded);
        response.setLevelUp(levelUp);
        response.setBonusQuestionTriggered(levelUp);
        response.setMessage(resolveGameMessage(isCorrect, concluded, levelUp, progress));
        return response;
    }

    private int currentProgressOf(StudentProgress progress) {
        return progress.getCurrentProgress() == null ? 0 : progress.getCurrentProgress();
    }

    // A concluded-FAILED question: stay in the sub-level (resetting its solve streak) or,
    // at the normal level, count toward dropping into the sub-level.
    private void onFailed(StudentProgress progress) {
        if (progress.getInSubLevel()) {
            progress.setSubLevelProgress(0);
        } else {
            progress.setConsecutiveFails(progress.getConsecutiveFails() + 1);
            if (progress.getConsecutiveFails() >= SUB_LEVEL_ENTER_FAILS) {
                progress.setInSubLevel(true);
                progress.setConsecutiveFails(0);
                progress.setSubLevelProgress(0);
            }
        }
    }

    private ProgressStatusResponse buildGameResponse(User user, StudentProgress progress, long total) {
        ProgressStatusResponse response = new ProgressStatusResponse();
        response.setSuccess(true);
        response.setCurrentLevel(progress.getCurrentLevel());
        response.setInSubLevel(progress.getInSubLevel());
        response.setLevelProgressCurrent(currentProgressOf(progress)); // frozen automatically in sub-level
        response.setLevelProgressTarget(LEVEL_UP_THRESHOLD);
        response.setTotalStars(user.getTotalStars());
        response.setTotalAttempts(total);
        response.setCurrentStreak(progress.getCurrentStreak());
        return response;
    }

    private String resolveGameMessage(boolean correct, String concluded, boolean levelUp, StudentProgress progress) {
        if (levelUp) {
            return "כל הכבוד! עלית רמה! 🚀 מחכה לך שאלת בונוס!";
        }
        if ("FAILED".equals(concluded)) {
            return progress.getInSubLevel()
                    ? "בוא נתרגל ברמה קלה יותר 🎯"
                    : "אל דאגה — הנה הפתרון. ננסה את הבאה!";
        }
        if (correct) {
            return progress.getInSubLevel() ? "כל הכבוד! עוד אחת 🌟" : "מצוין! תשובה נכונה ✅";
        }
        return "לא נורא, נסה שוב! 💡";
    }

    @Transactional
    public ProgressStatusResponse revealSolution(Long userId, Long subSubjectId, Long questionId) {
        // "Show full solution" / give up: reveal everything and mark the question FAILED.
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        question.setStatus(QuestionStatus.FAILED);
        questionRepository.save(question);

        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        onFailed(progress);
        progress.setCurrentStreak(0);
        progress.setActiveQuestionId(null);
        progressRepository.save(progress);

        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, subSubjectId);
        ProgressStatusResponse response = buildGameResponse(user, progress, total);
        response.setConcluded("FAILED");
        response.setMessage(progress.getInSubLevel()
                ? "בוא נתרגל ברמה קלה יותר 🎯"
                : "הנה הפתרון המלא. ננסה את הבאה!");
        return response;
    }

    @Transactional(readOnly = true)
    public ProgressStatusResponse getStatus(Long userId, Long subSubjectId) {
        // Snapshot for the game page (initial progress bar + level + stars).
        User user = resolveUser(userId);
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, subSubjectId);
        Optional<StudentProgress> progressOpt = progressRepository.findByUserIdAndSubSubjectId(userId, subSubjectId);
        if (progressOpt.isPresent()) {
            ProgressStatusResponse response =
                    buildGameResponse(user, progressOpt.get(), total);
            response.setMessage("בהצלחה! 💪");
            return response;
        }
        // No progress yet — defaults for a fresh sub-subject.
        ProgressStatusResponse response = new ProgressStatusResponse();
        response.setSuccess(true);
        response.setCurrentLevel(MIN_LEVEL);
        response.setInSubLevel(false);
        response.setLevelProgressCurrent(0);
        response.setLevelProgressTarget(LEVEL_UP_THRESHOLD);
        response.setTotalStars(user.getTotalStars());
        response.setTotalAttempts(0);
        response.setMessage("בוא נתחיל! 🚀");
        return response;
    }


    @Transactional
    public QuestionResponse getNextQuestion(Long userId, Long subSubjectId, String language, boolean multipleChoice) {
        // Difficulty comes from the student's level (sub-level forces the easiest). The
        // multipleChoice flag is derived from difficulty here, so the client renders MC
        // for easy questions and a typed box for the hardest.
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);
        StudentProgress progress = loadOrCreateProgress(user, subSubject);

        int difficultyLevel;
        int subSubjectLevel;
        if (progress.getInSubLevel()) {
            difficultyLevel = 1;
            subSubjectLevel = 1;
        } else {
            difficultyLevel = Math.min(progress.getCurrentLevel(), MAX_DIFFICULTY_LEVEL);
            subSubjectLevel = progress.getCurrentLevel();
        }
        // Difficulty ≤ 2 is always multiple choice; from difficulty 3 up it's a
        // fifty-fifty coin flip between multiple choice and a typed answer.
        boolean mc = difficultyLevel <= MC_MAX_DIFFICULTY || random.nextBoolean();

        // Cluster-aware step: fetch the student's ML cohort BEFORE generating, so both
        // generators can adapt the next question to the cohort's weakness / mastery.
        // Resume the same question on page reload — only generate a new one once resolved.
        Long activeId = progress.getActiveQuestionId();
        if (activeId != null) {
            Optional<Question> active = questionRepository.findById(activeId);
            if (active.isPresent() && active.get().getStatus() == QuestionStatus.CURRENT) {
                return buildQuestionResponse(active.get(), subSubjectId, null, displayNameOf(user));
            }
            // Stale pointer (question was resolved without clearing the field) — clear it.
            progress.setActiveQuestionId(null);
        }

        ClusterContext cluster = clusterContextService.forUser(userId);

        Question question = generateAdaptiveQuestion(user, subSubject, subSubjectLevel,
                difficultyLevel, language, mc, cluster);
        question = questionRepository.save(question);
        progress.setActiveQuestionId(question.getId());
        progressRepository.save(progress);
        // Archive so this question counts toward the student's history
        archiveQuestion(user, subSubject, question);
        return buildQuestionResponse(question, subSubjectId, null, displayNameOf(user));
    }

    /**
     * Picks the generation pipeline and feeds it the cluster context.
     * When AI questions are enabled it tries the AI generator first (passing the full
     * {@link ClusterContext}); on ANY failure (service down, no API key, bad output) it
     * falls back to the code-based generator so the student always gets a question.
     */
    private Question generateAdaptiveQuestion(User user, SubSubject subSubject, int subSubjectLevel,
                                              int difficultyLevel, String language, boolean mc,
                                              ClusterContext cluster) {
        Optional<SubSubjectAiConfig> aiConfig = subSubjectAiConfigRepository
                .findBySubSubjectId(subSubject.getId());

        boolean forceAi     = aiConfig.isPresent();
        boolean useAi       = forceAi || aiQuestionsEnabled;
        boolean effectiveMc = mc || aiConfig.map(SubSubjectAiConfig::isForceMultipleChoice).orElse(false);
        String  aiTopic     = aiConfig.map(SubSubjectAiConfig::getAiTopic).orElse(null);

        if (useAi && aiQuestionService != null) {
            try {
                int aiDifficulty = clampAiDifficulty(subSubjectLevel + cluster.getDifficultyBias());
                return aiQuestionService.generateQuestion(subSubject, aiTopic, aiQuestionTheme,
                        aiDifficulty, language, effectiveMc, cluster, user.getId());
            } catch (RuntimeException e) {
                if (forceAi) throw e; // AI-only sub-subjects have no code fallback
                log.warn("AI generation failed ({}); falling back to the code-based generator.", e.getMessage());
            }
        }
        return generateUniqueCodeQuestion(user, subSubject, subSubjectLevel, difficultyLevel,
                language, effectiveMc, cluster);
    }

    /**
     * Code-based generation with light deduplication: regenerate (up to
     * {@link #MAX_DEDUP_ATTEMPTS} times) until the expression is one the student hasn't
     * already seen for this sub-subject. Falls back to the last attempt if all collide.
     */
    private Question generateUniqueCodeQuestion(User user, SubSubject subSubject, int subSubjectLevel,
                                                int difficultyLevel, String language, boolean mc,
                                                ClusterContext cluster) {
        Set<String> seen = archiveRepository.findSeenExpressionsByUserAndSubSubject(
                user.getId(), subSubject.getId());

        Question question = createCodeQuestion(subSubject, subSubjectLevel, difficultyLevel, language, mc, cluster);
        for (int attempt = 1; attempt < MAX_DEDUP_ATTEMPTS && seen.contains(question.getExpression()); attempt++) {
            question = createCodeQuestion(subSubject, subSubjectLevel, difficultyLevel, language, mc, cluster);
        }
        return question;
    }

    /** Picks the code-based generator for the sub-subject's subject (Polynomial → algebra, else arithmetic). */
    private Question createCodeQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                        String language, boolean mc, ClusterContext cluster) {
        if (isPolynomial(subSubject)) {
            return polynomialGenerator.createQuestion(subSubject, subSubjectLevel, difficultyLevel, language, mc, cluster);
        }
        return calculationGenerator.createQuestion(subSubject, subSubjectLevel, difficultyLevel, language, mc, cluster);
    }

    /** Generator routing without a cluster context (bonus / initial-assessment paths). */
    private Question createCodeQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                        String language, boolean mc) {
        if (isPolynomial(subSubject)) {
            return polynomialGenerator.createQuestion(subSubject, subSubjectLevel, difficultyLevel, language, mc);
        }
        return calculationGenerator.createQuestion(subSubject, subSubjectLevel, difficultyLevel, language, mc);
    }

    private boolean isPolynomial(SubSubject subSubject) {
        return subSubject.getSubject() != null
                && "Polynomial".equalsIgnoreCase(subSubject.getSubject().getName());
    }

    private int clampAiDifficulty(int difficulty) {
        return Math.max(AI_MIN_DIFFICULTY, Math.min(difficulty, AI_MAX_DIFFICULTY));
    }

    @Transactional
    public QuestionResponse getInitialAssessmentQuestion(Long userId, InitialAssessmentRequest request) {
       //משתמשים בשאלון הגדרת רמה בהרשמה כדי לקחת תלמיד לרמה המתאימה לו
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

        Question question = createCodeQuestion(subSubject, subSubjectLevel, difficultyLevel, language, true);
        question = questionRepository.save(question);

        archiveQuestion(user, subSubject, question);
        return buildQuestionResponse(question, subSubject.getId(), null, displayNameOf(user));
    }

    private int mapGradeToLevel(Integer grade) {//לפי כיתה
        if (grade == null || grade <= 3){
            return 1;
        }
        if (grade <= 6){
            return 2;
        }
        return 3;
    }

    private int mapConfidenceToLevel(String confidence) {//לפי ביטחון תלמיד
        if (confidence == null) return 2;
        switch (confidence.toUpperCase()) {
            case "EASY": {
                return 1;
            }
            case "HARD": {
                return 3;
            }
            case "MEDIUM":
            default: {
                return 2;
            }
        }
    }


    @Transactional
    public BonusQuestionResponse getBonusQuestion(Long userId, Long subSubjectId, String language) {
        //שאלת אתגר בונוס ייחודית שעולים שלב ומניעת כפילות שאלות ומבקשים רמת קושי הכי קשה לתת נושא
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);
        StudentProgress progress = loadOrCreateProgress(user, subSubject);

        int currentLevel = progress.getCurrentLevel();
        // Bonus is meant to be harder than normal play: one difficulty band above the
        // student's current level, capped at the top of the 1–10 scale.
        int bonusDifficulty = Math.min(currentLevel + 1, MAX_DIFFICULTY_LEVEL);

        //Deduplication: fetch expressions already shown to this user
        Set<String> seenExpressions = archiveRepository.findSeenExpressionsByUserAndSubSubject(userId, subSubject.getId());

        //Generate, retrying until a fresh expression is found
        Question question = null;
        for (int attempt = 0; attempt < MAX_BONUS_GEN_ATTEMPTS; attempt++) {
            Question candidate = createCodeQuestion(subSubject, currentLevel, bonusDifficulty, language, true);
            if (!seenExpressions.contains(candidate.getExpression())) {
                question = candidate;
                break;
            }
        }
        // Pool exhausted — accept the last candidate rather than blocking
        if (question == null) {
            question = createCodeQuestion(subSubject, currentLevel, bonusDifficulty, language, true);
        }
        question = questionRepository.save(question);
        // Archive so the next bonus call won't show the same expression again
        archiveQuestion(user, subSubject, question);
        return buildBonusQuestionResponse(question, subSubjectId, displayNameOf(user));
    }


    @Transactional
    public ProgressStatusResponse submitBonusAnswer(Long userId, SubmitAnswerRequest request) {
        // Bonus answers now flow through the SAME tracking path as normal answers: correctness is
        // graded server-side and the attempt is recorded into exercise_attempts, so the bonus question
        // also feeds the ML clustering (and the +50 stars can't be claimed without actually answering).
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(request.getSubSubjectId());
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        boolean correct = question.getCorrectAnswer().equals(request.getUserAnswer());
        String errorPattern = !correct
                ? errorPatternService.analyze(subSubject, question.getExpression(), question.getCorrectAnswer(),
                                      request.getUserAnswer(), request.getQuestionType())
                : null;
        saveAttempt(user, subSubject, request.getQuestionId(), correct, request.getCurrentDifficulty(),
                request.getQuestionType(), request.getUserAnswer(), errorPattern, LocalDateTime.now());

        if (correct) {
            user.setTotalStars(user.getTotalStars() + BonusQuestionResponse.BONUS_STARS);
            userRepository.save(user);
        }
        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, request.getSubSubjectId());
        ProgressStatusResponse response = buildGameResponse(user, progress, total);
        response.setAnswerCorrect(correct);
        response.setMessage(correct
                ? "מעולה! +" + BonusQuestionResponse.BONUS_STARS + " כוכבים! 🌟"
                : "לא נורא! נמשיך 💪");
        return response;
    }

    // Pure progression-analysis helpers. Kept package-private because they are independently
    // unit-tested and can be reused by dashboard/adaptation flows without persistence coupling.
    SpaceshipStatus evaluateSpaceshipStatus(long total, long correctWindow, long correctRecent) {
        if (total >= LEVEL_UP_WINDOW && correctWindow >= LEVEL_UP_THRESHOLD) {
            return SpaceshipStatus.BOOSTING;
        }
        if (total >= INTERMEDIATE_WINDOW && correctRecent < INTERMEDIATE_THRESHOLD) {
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
            default:
                progress.setCurrentProgress(currentProgressOf(progress) + 1);
        }
    }

    String detectWeakness(List<ExerciseAttempt> attempts) {
        Map<String, Integer> wrongByType = new HashMap<>();
        for (ExerciseAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getIsCorrect())) continue;
            String key = attempt.getErrorPattern() != null
                    ? attempt.getErrorPattern() : attempt.getQuestionType();
            if (key != null) wrongByType.merge(key, 1, Integer::sum);
        }
        return wrongByType.entrySet().stream()
                .filter(entry -> entry.getValue() >= WEAKNESS_MIN_SAMPLE)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    private void archiveQuestion(User user, SubSubject subSubject, Question question) {
        //שומרים כל שאלה שהוצגה לתלמיד בטבלה של ארכיון שאלות
        // 1. יצירת עותקים בטוחים של הרשימות (מניעת Shared References)
        List<String> solutionCopy = (question.getSolution() != null)
                ? new ArrayList<>(question.getSolution())
                : new ArrayList<>();

        List<String> optionsCopy = (question.getOptions() != null)
                ? new ArrayList<>(question.getOptions())
                : new ArrayList<>();

        // 2. בניית האובייקט עם הרשימות עצמן + חותמת זמן (מתי השאלה נשמרה/הוצגה)
        QuestionArchive archive = new QuestionArchive(user, subSubject, question.getExpression(),
                question.getCorrectAnswer(), solutionCopy, optionsCopy ,// מעבירים List
                question.getLanguage(), question.getDifficultyLevel(), LocalDateTime.now());
        archiveRepository.save(archive);
    }

    private QuestionResponse buildQuestionResponse(Question question, Long subSubjectId,
                                                   String weakness, String displayName) {
        QuestionResponse response = new QuestionResponse();
        populateQuestionFields(response, question, subSubjectId, displayName);
        response.setRecommendedQuestionType(weakness);
        response.setMessage(weakness != null
                ? "נחש! אתה מתאמן על: " + formatQuestionType(weakness)
                : "שאלה חדשה מחכה לך! 💪");
        return response;
    }

    private BonusQuestionResponse buildBonusQuestionResponse(Question question, Long subSubjectId,
                                                             String displayName) {
        BonusQuestionResponse response = new BonusQuestionResponse();
        populateQuestionFields(response, question, subSubjectId, displayName);
        response.setMessage("🌟 שאלת בונוס! ענה נכון וקבל " + BonusQuestionResponse.BONUS_STARS + " כוכבים!");
        return response;
    }

    private void populateQuestionFields(QuestionResponse response, Question question,
                                        Long subSubjectId, String displayName) {
        String solutionText = question.getSolution() == null
                ? "" : question.getSolution().stream().collect(Collectors.joining("\n"));

        response.setSuccess(true);
        response.setQuestionId(question.getId());
        response.setExpression(personalize(question.getExpression(), displayName));
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setSolution(personalize(solutionText, displayName));
        // Join with '|' (not ',') so an option may itself contain commas, e.g. "X1=5 , X2=10".
        response.setOptions(question.getOptions() == null ? null : String.join("|", question.getOptions()));
        response.setDifficultyLevel(question.getDifficultyLevel());
        response.setSubSubjectId(subSubjectId);
    }

    /**
     * Replaces the reusable NAME placeholder with the student's display name.
     * Applied only to the outgoing response — the stored/cached question keeps NAME
     * so it stays reusable across students.
     */
    private String personalize(String text, String displayName) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("NAME", displayName);
    }

    /** Student's display name for personalising questions; falls back to a neutral default. */
    private String displayNameOf(User user) {
        return (user.getFullName() == null || user.getFullName().isBlank())
                ? "Student" : user.getFullName().trim();
    }

    private void saveAttempt(User user, SubSubject subSubject, Long questionId,
                             Boolean isCorrect, Integer difficultyLevel,
                             String questionType, String userAnswer,
                             String errorPattern, LocalDateTime answeredAt) {

        // בגלל שכל הנתונים כבר מועברים כפרמטרים, אנחנו פשוט בונים את האובייקט ושומרים
        ExerciseAttempt attempt = new ExerciseAttempt(
                user,
                subSubject,
                questionId,
                isCorrect,
                difficultyLevel,
                questionType,
                userAnswer,
                errorPattern,
                answeredAt
        );

        attemptRepository.save(attempt);
    }


    private StudentProgress loadOrCreateProgress(User user, SubSubject subSubject) {
        //בלי המתודה הזו המערכת לא הייתה יודעת אם התלמיד הוא תלמיד חדש שצריך להתחיל מההתחלה,
        // או תלמיד ותיק שצריך להמשיך מהמקום שבו הוא עצר
        Optional<StudentProgress> progressOptional =
                progressRepository.findByUserIdAndSubSubjectId(user.getId(), subSubject.getId());
        if (progressOptional.isPresent()) {
            StudentProgress progress = progressOptional.get();
            if (Boolean.TRUE.equals(progress.getActive())) {
                return progress;
            }
        }
        return createNewProgress(user, subSubject);
    }


    private StudentProgress createNewProgress(User user, SubSubject subSubject) {
        //כל תלמיד שמתחיל נושא חדש מקבל ערכים נורמלים רגילים
        return progressRepository.save(new StudentProgress(user, subSubject, MIN_LEVEL, 0, true));
    }


    private User resolveUser(Long userId) {
        //מבטיחה שהמערכת עובדת רק עם משתמשים שבאמת קיימים במסד הנתונים
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        return userOptional.get();
    }


    private SubSubject resolveSubSubject(Long subSubjectId) {
        // לוודא שהנושא שהתלמיד מנסה ללמוד אכן קיים במערכת לפני שאנחנו מנסים לבצע עליו פעולות
        Optional<SubSubject> subOptional = subSubjectRepository.findById(subSubjectId);
        if (subOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SubSubject not found: " + subSubjectId);
        }
        return subOptional.get();
    }


    private String formatQuestionType(String questionType) {
        //מתודת תרגום
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

}
