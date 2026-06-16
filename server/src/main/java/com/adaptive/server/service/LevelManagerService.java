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
public class LevelManagerService {//מוח שמנהל התקדמות תלמיד מחלקה חשובה

    static final int LEVEL_UP_WINDOW = 30; //חלון עליית רמה
    static final int LEVEL_UP_THRESHOLD = 24; // ≥ 80 %
    static final int INTERMEDIATE_WINDOW = 10; // חלון רמת ביניים
    static final int INTERMEDIATE_THRESHOLD =  6; // < 60 %
    static final int WEAKNESS_WINDOW = 15; //זיהוי חולשות
    static final double WEAKNESS_ERROR_RATE = 0.50;//אם תלמיד טעה ב50 אחוז שאלות אז נסמן כחולשה ונייצר תרגילים שמתמקדים בנושא הזה
    static final int WEAKNESS_MIN_SAMPLE = 4;//מדגם מינימלי אם נגיד טעה ב4 אז נכריז חולשה
    private static final int MIN_LEVEL = 1;//רמה מינימלית שלא נרד לרמה 0

    private static final String CALCULATION_SUBJECT_NAME = "Calculation"; //אם בעתיד נרצה לשנות את השם במסד הנתונים, נצטרך לשנות אותו רק בשורה הזו והוא יתעדכן אוטומטית בכל הפרויקט

    static final int MAX_BONUS_GEN_ATTEMPTS = 5;//המערכת תנסה להגריל שאלה חדשה מקסימום 5 פעמים זה מונע לולאה אינסופית

    private final QuestionRepository questionRepository;
    private final QuestionArchiveRepository archiveRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final StudentProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final CalculationGenerator calculationGenerator;

    public LevelManagerService(ExerciseAttemptRepository attemptRepository, StudentProgressRepository progressRepository,
                               UserRepository userRepository, SubSubjectRepository subSubjectRepository,
                               CalculationGenerator calculationGenerator, QuestionRepository questionRepository,
                               QuestionArchiveRepository archiveRepository) {
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.calculationGenerator = calculationGenerator;
        this.questionRepository = questionRepository;
        this.archiveRepository = archiveRepository;
    }


    @Transactional
    public ProgressStatusResponse submitAnswer(Long userId, SubmitAnswerRequest request) {
        //המתודה הזו לוקחת את מה שעשית בודקת איך זה מסתדר עם מה שעשית בעבר,
        // ומחליטה מה הצעד הבא הכי טוב עבורך – אם זה לעלות רמה, להישאר באותו מקום, או לקבל תגבור
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(request.getSubSubjectId());

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        boolean isCorrect = question.getCorrectAnswer().equals(request.getUserAnswer());
        // קריאה למתודה המעודכנת שדורשת את ה-question וה-userAnswer
        String calculatedErrorPattern = !isCorrect ?
                analyzeErrorPattern(question.getExpression(), question.getCorrectAnswer(), request.getUserAnswer(), request.getQuestionType())
                : null;
        saveAttempt(user, subSubject, request.getQuestionId(), isCorrect, request.getCurrentDifficulty(),
                request.getQuestionType(), request.getUserAnswer(), calculatedErrorPattern, LocalDateTime.now());
        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, request.getSubSubjectId());
        List<ExerciseAttempt> last30 = fetchRecent(userId, request.getSubSubjectId(), LEVEL_UP_WINDOW);

        long correct30 = 0;
        long correct10 = 0;
        for (int i = 0; i < last30.size(); i++) {
            if (Boolean.TRUE.equals(last30.get(i).getIsCorrect())) {
                correct30++;
                if (i < INTERMEDIATE_WINDOW) {
                    correct10++;
                }
            }
        }
        List<ExerciseAttempt> weaknessWindow = new ArrayList<>();
        int limit = Math.min(last30.size(), WEAKNESS_WINDOW);
        for (int i = 0; i < limit; i++) {
            weaknessWindow.add(last30.get(i));
        }
        SpaceshipStatus status = evaluateSpaceshipStatus(total, correct30, correct10);
        applyStatusToProgress(progress, status);
        progressRepository.save(progress);
        String weakness = detectWeakness(weaknessWindow);
        return buildResponse(user, progress, status, weakness, isCorrect, correct10, correct30, total);
    }

        @Transactional(readOnly = true)
        public ProgressStatusResponse getStatus(Long userId, Long subSubjectId) {
        //אם מראים לתלמיד דוח התקדמות כי הוא לומד או הודעת פתיחה אם חדש בנושא
            Optional<StudentProgress> progressOpt = progressRepository.findByUserIdAndSubSubjectId(userId, subSubjectId);
            if (progressOpt.isPresent()) {
                StudentProgress progress = progressOpt.get();
                if (Boolean.TRUE.equals(progress.getActive())) {
                    return buildStatusFromHistory(progress, userId, subSubjectId);
                }
            }
            // אם לא נמצא או לא פעיל, מחזירים ברירת מחדל
            return buildDefaultStatus();
        }


    @Transactional
    public QuestionResponse getNextQuestion(Long userId, Long subSubjectId, String language, boolean multipleChoice) {
        //בודקים רמת תלמיד ונקודות חולשה ולפי זה לוקחים ממחולל שאלה מתאימה ושומרים אותה בארכיון שמערכת לא תשכח
        User user = resolveUser(userId);
        SubSubject subSubject = resolveSubSubject(subSubjectId);

        StudentProgress progress = loadOrCreateProgress(user, subSubject);
        int difficultyLevel = progress.getCurrentLevel();
        int subSubjectLevel = progress.getCurrentLevel();

        List<ExerciseAttempt> last15 = fetchRecent(userId, subSubjectId, WEAKNESS_WINDOW);
        String weakness = detectWeakness(last15);

        SubSubject targetSubSubject = resolveTargetSubSubject(subSubject, weakness);

        Question question = calculationGenerator.createQuestion(targetSubSubject, subSubjectLevel,
                difficultyLevel, language, multipleChoice);
        question = questionRepository.save(question);
        // Archive so this question counts toward the student's history
        archiveQuestion(user, targetSubSubject, question);
        return buildQuestionResponse(question, subSubjectId, weakness);
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

        Question question = calculationGenerator.createQuestion(subSubject, subSubjectLevel, difficultyLevel,
                language, true);
        question = questionRepository.save(question);

        archiveQuestion(user, subSubject, question);
        return buildQuestionResponse(question, subSubject.getId(), null);
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
        int bonusDifficulty = calculationGenerator.getMaxDifficultyLevelForSubSubject(subSubject);

        //Deduplication: fetch expressions already shown to this user
        Set<String> seenExpressions = archiveRepository.findSeenExpressionsByUserAndSubSubject(userId, subSubject.getId());

        //Generate, retrying until a fresh expression is found
        Question question = null;
        for (int attempt = 0; attempt < MAX_BONUS_GEN_ATTEMPTS; attempt++) {
            Question candidate = calculationGenerator.createQuestion(subSubject, currentLevel,
                    bonusDifficulty, language, true);
            if (!seenExpressions.contains(candidate.getExpression())) {
                question = candidate;
                break;
            }
        }
        // Pool exhausted — accept the last candidate rather than blocking
        if (question == null) {
            question = calculationGenerator.createQuestion(subSubject, currentLevel,
                    bonusDifficulty, language, true);
        }
        question = questionRepository.save(question);
        // Archive so the next bonus call won't show the same expression again
        archiveQuestion(user, subSubject, question);
        return buildBonusQuestionResponse(question, subSubjectId);
    }


    @Transactional
    public ProgressStatusResponse submitBonusAnswer(Long userId, Long subSubjectId, boolean correct) {
        //מנהלת את הבונוס אם יש כוכבים או אין
        User user = resolveUser(userId);
        if (correct) {
            user.setTotalStars(user.getTotalStars() + BonusQuestionResponse.BONUS_STARS);
            userRepository.save(user);
        }
        //  שליפת ההתקדמות ממסד הנתונים
        Optional<StudentProgress> progressOptional = progressRepository.findByUserIdAndSubSubjectId(userId, subSubjectId);
        if (progressOptional.isPresent()) {//האם התקדמות פועלת והאם פעילה
            StudentProgress progress = progressOptional.get();
            if (Boolean.TRUE.equals(progress.getActive())) {
                // אם האובייקט גם קיים וגם פעיל - מחזירים סטטוס היסטוריה
                return buildStatusFromHistory(progress, userId, subSubjectId, user);
            }
        }
        // אם ההתקדמות לא הייתה קיימת, או שהיא קיימת אבל לא פעילה - נגיע לכאן
        return buildDefaultStatus(user);
    }


    SpaceshipStatus evaluateSpaceshipStatus(long total, long correct30, long correct10) {
        //סטטוס חללית
        if (total >= LEVEL_UP_WINDOW && correct30 >= LEVEL_UP_THRESHOLD) {
            return SpaceshipStatus.BOOSTING;
        }
        if (total >= INTERMEDIATE_WINDOW && correct10 < INTERMEDIATE_THRESHOLD) {
            return SpaceshipStatus.STOPPED;
        }
        return SpaceshipStatus.MOVING;
    }

    void applyStatusToProgress(StudentProgress progress, SpaceshipStatus status) {
        //עדכון פיזי של התקדמות תלמיד
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

    //לאתר איפה תלמיד מתקשה ומשנה סוגי שאלות במחולל שלא ייתאש התלמיד
    String detectWeakness(List<ExerciseAttempt> attempts) {
        if (attempts.isEmpty()) return null;
        // שלב 1: איסוף נתונים לתוך מפה (מתודה נפרדת)
        Map<String, TypeStats> stats = collectStats(attempts);
        // שלב 2: ניתוח המפה למציאת החולשה (מתודה נפרדת)
        return findWorstWeakness(stats);
    }

    // מתודה נפרדת לאיסוף הנתונים
    private Map<String, TypeStats> collectStats(List<ExerciseAttempt> attempts) {
        Map<String, TypeStats> stats = new HashMap<>();
        for (ExerciseAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getIsCorrect())) {
                continue;
            }
            String key = attempt.getErrorPattern() != null ? attempt.getErrorPattern() : attempt.getQuestionType();
            if (key == null) continue;
            stats.computeIfAbsent(key, k -> new TypeStats()).record(true);
        }
        return stats;
    }

    // מתודה נפרדת לחיפוש החולשה
    private String findWorstWeakness(Map<String, TypeStats> stats) {
        String maxWeakness = null;
        int maxWrong = -1;
        for (Map.Entry<String, TypeStats> entry : stats.entrySet()) {
            if (entry.getValue().wrong >= WEAKNESS_MIN_SAMPLE && entry.getValue().wrong > maxWrong) {
                maxWrong = entry.getValue().wrong;
                maxWeakness = entry.getKey();
            }
        }
        return maxWeakness;
    }


    private SubSubject resolveTargetSubSubject(SubSubject subSubject, String weakness) {
        //איזה נושא לימוד נביא לתלמיד שמתקשה בנושא מסוים
        if (weakness != null) {
            CalculationOperation fromWeakness = questionTypeToOperation(weakness);
            if (fromWeakness != null) {
                SubSubject weaknessSubSubject = subSubjectRepository
                        .findByNameAndSubject_Name(fromWeakness.getSubSubjectName(),
                                CALCULATION_SUBJECT_NAME);
                if (weaknessSubSubject != null) {
                    return weaknessSubSubject;
                }
            }
        }
        return subSubject;
    }

    private CalculationOperation questionTypeToOperation(String questionType) {
        //מיפוי - תרגום של שמות סוגי שאלות לבין פעולה חשבונית
        if (questionType == null) {
            return null;
        }
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


    private void archiveQuestion(User user, SubSubject subSubject, Question question) {
        //שומרים כל שאלה שהוצגה לתלמיד בטבלה של ארכיון שאלות
        // 1. יצירת עותקים בטוחים של הרשימות (מניעת Shared References)
        List<String> solutionCopy = (question.getSolution() != null)
                ? new ArrayList<>(question.getSolution())
                : new ArrayList<>();

        List<String> optionsCopy = (question.getOptions() != null)
                ? new ArrayList<>(question.getOptions())
                : new ArrayList<>();

        // 2. בניית האובייקט עם הרשימות עצמן
        QuestionArchive archive = new QuestionArchive(user, subSubject, question.getExpression(),
                question.getCorrectAnswer(), solutionCopy, optionsCopy ,// מעבירים List
                question.getLanguage(), question.getDifficultyLevel());
        archiveRepository.save(archive);
    }

    private ProgressStatusResponse buildResponse(User user, StudentProgress progress, SpaceshipStatus status,
                                                 String weakness, boolean lastAnswerCorrect,
                                                 long correct10, long correct30, long total) {
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
        response.setTotalStars(user.getTotalStars());
        response.setBonusQuestionTriggered(isLevelUp);
        response.setMessage(resolveMessage(status, weakness, lastAnswerCorrect));
        return response;
    }

    private ProgressStatusResponse buildStatusFromHistory(StudentProgress progress, Long userId, Long subSubjectId) {
        //היא חוסכת כתיבה כפולה של הלוגיקה של שליפת המשתמש בכל מקום שבו אנחנו רוצים לבנות סטטוס התקדמות מתקציר ההיסטוריה.
        User user = resolveUser(userId);
        return buildStatusFromHistory(progress, userId, subSubjectId, user);
    }

    private ProgressStatusResponse buildStatusFromHistory(StudentProgress progress, Long userId,
                                                          Long subSubjectId, User user) {
        //סיכום מצב קריאה בלבד, איפה תלמיד עומד כרגע
        long total = attemptRepository.countByUserIdAndSubSubjectId(userId, subSubjectId);
        List<ExerciseAttempt> last30 = fetchRecent(userId, subSubjectId, LEVEL_UP_WINDOW);

        long correct30 = countCorrect(last30);
        long correct10 = countCorrect(slice(last30, INTERMEDIATE_WINDOW));
        SpaceshipStatus status = evaluateSpaceshipStatus(total, correct30, correct10);
        String weakness = detectWeakness(slice(last30, WEAKNESS_WINDOW));

        ProgressStatusResponse response = new ProgressStatusResponse();
        response.setSuccess(true);
        response.setCurrentLevel(progress.getCurrentLevel());
        response.setSpaceshipStatus(status.name());
        response.setWeaknessType(weakness);
        response.setCorrectLast10((int) correct10);
        response.setCorrectLast30((int) correct30);
        response.setTotalAttempts(total);
        response.setTotalStars(user.getTotalStars());
        response.setMessage("המשך לתרגל — אתה עושה עבודה מצוינת! ⭐");
        return response;
    }

    private ProgressStatusResponse buildDefaultStatus() {
        //חוסכת כתיבת NULL כשרוצים לאתחל מצב בסיסי
        return buildDefaultStatus(null);
    }

    private ProgressStatusResponse buildDefaultStatus(User user) {
        //יוצרים אובייקט תגובה התחלתי
        ProgressStatusResponse response = new ProgressStatusResponse();
        response.setSuccess(true);
        response.setCurrentLevel(MIN_LEVEL);
        response.setSpaceshipStatus(SpaceshipStatus.MOVING.name());
        response.setCorrectLast10(0);
        response.setCorrectLast30(0);
        response.setTotalAttempts(0);
        response.setTotalStars(user != null ? user.getTotalStars() : 0);
        response.setMessage("עוד לא התחלת נושא זה. בוא נתחיל! 🚀");
        return response;
    }


    private QuestionResponse buildQuestionResponse(Question question, Long subSubjectId, String weakness) {
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

    private void populateQuestionFields(QuestionResponse response, Question question, Long subSubjectId) {
        String solutionText = question.getSolution() == null
                ? "" : question.getSolution().stream().collect(Collectors.joining("\n"));

        response.setSuccess(true);
        response.setQuestionId(question.getId());
        response.setExpression(question.getExpression());
        response.setCorrectAnswer(question.getCorrectAnswer());
        response.setSolution(solutionText);
        response.setOptions(question.getOptions() == null ? null : String.join(",", question.getOptions()));
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


    private String analyzeErrorPattern(String expression, String correctAnswer, String userAnswer, String questionType) {
        //מנסים להבין למה תלמיד טעה - זה יכול לעזור בעתיד לADMIN
        if (userAnswer == null || userAnswer.trim().isEmpty()) return "EMPTY_ANSWER";
        try {
            int userVal = Integer.parseInt(userAnswer.trim());
            int correctVal = Integer.parseInt(correctAnswer.trim());
            if (questionType != null && questionType.contains("SUBTRACTION")) {
                String[] parts = expression.split("-");
                if (parts.length == 2) {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    if (userVal == (a + b)) return "CONFUSED_SUB_WITH_ADD";
                }
            }
            if (Math.abs(userVal - correctVal) <= 2) return "MINOR_CALCULATION_ERROR";
        } catch (NumberFormatException e) {
            return "INVALID_FORMAT_ERROR";
        }
        return "GENERAL_ERROR_" + questionType;
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


    private List<ExerciseAttempt> fetchRecent(Long userId, Long subSubjectId, int limit) {
        //מסתכלים על מה תלמיד עשה לאחרונה לפי זמן תשובה-רלוונטיים ביותר
        return attemptRepository.findByUserIdAndSubSubjectId(userId, subSubjectId,
                PageRequest.of(0, limit, Sort.by("answeredAt").descending()));
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


    private long countCorrect(List<ExerciseAttempt> attempts) {
        //סיכום של ביצועים של תלמיד - שנדע רמת הצלחה של תלמיד
        long count = 0;
        for (ExerciseAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getIsCorrect())) {
                count++;
            }
        }
        return count;
    }


    private List<ExerciseAttempt> slice(List<ExerciseAttempt> list, int max) {
        //לקחת רשימה של תרגילים ולהחזיר רק את ה-X הראשונים מתוכה, בלי לגרום לקריסה אם ביקשת יותר ממה שיש
        if (list.size() > max) {
            // מחזירים רק חלק מהרשימה (מההתחלה ועד max)
            return list.subList(0, max);
        }
        // אם הרשימה קטנה מהמקסימום, מחזירים את כל מה שיש בה
        return list;
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


        static class TypeStats {
        //מונה חכם שיודע לחשב אחוז שגיאות של תלמיד בנושא מסוים
            int total = 0;
            int wrong = 0;
            // מתודה שמקבלת פרמטר ומעדכנת את הנתונים
            void record(boolean isWrong) {
                // תמיד מוסיפים 1 לסך הכל השאלות שראינו
                this.total = this.total + 1;
                // אם התשובה הייתה שגויה, מוסיפים 1 למונה הטעויות
                if (isWrong == true) {
                    this.wrong = this.wrong + 1;
                }
            }
            // מתודה שמחשבת את האחוז
            double errorRate() {
                // מונעים חילוק באפס (אם עוד לא פתרנו שאלות)
                if (this.total == 0) {
                    return 0.0;
                }
                // מבצעים חילוק רגיל ומחזירים את האחוז
                double rate = (double) this.wrong / this.total;
                return rate;
            }
        }

}
