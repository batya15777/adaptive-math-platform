package com.adaptive.server.service;

import com.adaptive.server.DTOs.InitialAssessmentRequest;
import com.adaptive.server.DTOs.SubmitAnswerRequest;
import org.mockito.ArgumentCaptor;
import com.adaptive.server.entity.ExerciseAttempt;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionArchive;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.entity.enums.SpaceshipStatus;
import com.adaptive.server.repository.*;
import com.adaptive.server.responses.BonusQuestionResponse;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.responses.QuestionResponse;
import com.adaptive.server.service.QuestionsGenerators.CalculationGenerator;
import com.adaptive.server.service.QuestionsGenerators.PolynomialGenerator;
import com.adaptive.server.service.QuestionsGenerators.ClusterContext;
import com.adaptive.server.service.errorpattern.ArithmeticErrorAnalyzer;
import com.adaptive.server.service.errorpattern.ErrorPatternService;
import com.adaptive.server.service.errorpattern.PolynomialErrorAnalyzer;
import com.adaptive.server.service.errorpattern.VerbalErrorAnalyzer;
import com.adaptive.server.service.sse.AdminSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LevelManagerServiceTest {

    private ExerciseAttemptRepository  attemptRepository;
    private StudentProgressRepository  progressRepository;
    private UserRepository             userRepository;
    private SubSubjectRepository       subSubjectRepository;
    private SubSubjectAiConfigRepository subSubjectAiConfigRepository;
    private CalculationGenerator       calculationGenerator;
    private PolynomialGenerator        polynomialGenerator;
    private QuestionRepository         questionRepository;
    private QuestionArchiveRepository  archiveRepository;   // NEW
    private ClusterContextService      clusterContextService; // NEW (cluster-aware generation)
    private AiQuestionService          aiQuestionService;     // NEW (cluster-aware generation)
    private AdminSseService            adminSseService;

    private LevelManagerService service;

    private static final Long USER_ID        = 1L;
    private static final Long SUB_SUBJECT_ID = 10L;

    private User       testUser;
    private SubSubject testSubSubject;

    @BeforeEach
    void setUp() {
        attemptRepository    = mock(ExerciseAttemptRepository.class);
        progressRepository   = mock(StudentProgressRepository.class);
        userRepository       = mock(UserRepository.class);
        subSubjectRepository = mock(SubSubjectRepository.class);
        subSubjectAiConfigRepository = mock(SubSubjectAiConfigRepository.class);
        calculationGenerator = mock(CalculationGenerator.class);
        polynomialGenerator = mock(PolynomialGenerator.class);
        questionRepository   = mock(QuestionRepository.class);
        archiveRepository    = mock(QuestionArchiveRepository.class);
        clusterContextService = mock(ClusterContextService.class);
        aiQuestionService     = mock(AiQuestionService.class);
        adminSseService       = mock(AdminSseService.class);
        // Default: students have no cluster, so generation behaves exactly as before.
        when(clusterContextService.forUser(any())).thenReturn(ClusterContext.neutral());

        service = new LevelManagerService(
                attemptRepository, progressRepository,
                userRepository, subSubjectRepository,
                subSubjectAiConfigRepository,
                calculationGenerator, polynomialGenerator, questionRepository,
                archiveRepository, clusterContextService, aiQuestionService,
                new ErrorPatternService(
                        List.of(new ArithmeticErrorAnalyzer(), new PolynomialErrorAnalyzer(), new VerbalErrorAnalyzer()),
                        new ArithmeticErrorAnalyzer()),
                adminSseService);

        testUser = mock(User.class);
        when(testUser.getId()).thenReturn(USER_ID);
        when(testUser.getTotalStars()).thenReturn(0);

        testSubSubject = mock(SubSubject.class);
        when(testSubSubject.getId()).thenReturn(SUB_SUBJECT_ID);
        when(testSubSubject.getName()).thenReturn("add");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subSubjectRepository.findById(SUB_SUBJECT_ID)).thenReturn(Optional.of(testSubSubject));

        // Default archive stubs: empty seen-set, save returns arg
        when(archiveRepository.findSeenExpressionsByUserAndSubSubject(any(), any()))
                .thenReturn(Collections.emptySet());
        when(archiveRepository.save(any(QuestionArchive.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Default question stub so submitAnswer doesn't throw "Question not found"
        when(questionRepository.findById(99L)).thenReturn(Optional.of(stubSavedQuestion(1)));
    }


    @Test
    @DisplayName("Service instantiates without exceptions")
    void serviceInstantiates() {
        assertNotNull(service);
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("evaluateSpaceshipStatus()")
    class EvaluateSpaceshipStatus {

        @Test @DisplayName("Returns MOVING when total < LEVEL_UP_WINDOW (16)")
        void belowLevelUpWindow_isMoving() {
            // 15 < 16 → not boosting; 15 ≥ 10 but 10 correct ≥ 6 → not stopped → MOVING
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(15, 15, 10));
        }

        @Test @DisplayName("Returns BOOSTING when ≥30 attempts and ≥24 correct in last 30")
        void thirtyAttemptsWithHighAccuracy_isBoosting() {
            assertEquals(SpaceshipStatus.BOOSTING, service.evaluateSpaceshipStatus(30, 24, 8));
        }

        @Test @DisplayName("Returns BOOSTING only at the exact threshold of 24/30")
        void exactThreshold_isBoosting() {
            assertEquals(SpaceshipStatus.BOOSTING, service.evaluateSpaceshipStatus(30, 24, 10));
        }

        @Test @DisplayName("Returns MOVING when window reached but correct < LEVEL_UP_THRESHOLD (12)")
        void justBelowBoostThreshold_isMoving() {
            // 16 ≥ 16 but 11 correct < 12 → not boosting; 8 correct ≥ 6 → not stopped → MOVING
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(16, 11, 8));
        }

        @Test @DisplayName("Returns STOPPED when ≥10 attempts and <6 correct in last 10")
        void tenAttemptsWithLowAccuracy_isStopped() {
            assertEquals(SpaceshipStatus.STOPPED, service.evaluateSpaceshipStatus(10, 5, 5));
        }

        @Test @DisplayName("Returns STOPPED at the boundary of 5/10 correct")
        void boundaryStop_exactlyFiveCorrect() {
            assertEquals(SpaceshipStatus.STOPPED, service.evaluateSpaceshipStatus(10, 3, 5));
        }

        @Test @DisplayName("Returns MOVING when 10 attempts but ≥6 correct")
        void tenAttemptsWithOkAccuracy_isMoving() {
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(10, 8, 6));
        }

        @Test @DisplayName("BOOSTING check takes priority over STOPPED check")
        void boostingTakesPriorityOverStopped() {
            assertEquals(SpaceshipStatus.BOOSTING, service.evaluateSpaceshipStatus(30, 24, 3),
                    "BOOSTING condition is evaluated before STOPPED");
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("applyStatusToProgress()")
    class ApplyStatusToProgress {

        @Test @DisplayName("BOOSTING increments currentLevel by 1 and resets counter")
        void boosting_incrementsLevel() {
            StudentProgress p = progressAt(3);
            service.applyStatusToProgress(p, SpaceshipStatus.BOOSTING);
            assertEquals(4, p.getCurrentLevel());
            assertEquals(0, p.getCurrentProgress());
        }

        @Test @DisplayName("BOOSTING from level 1 goes to level 2")
        void boosting_fromLevelOne_goesToLevelTwo() {
            StudentProgress p = progressAt(1);
            service.applyStatusToProgress(p, SpaceshipStatus.BOOSTING);
            assertEquals(2, p.getCurrentLevel());
        }

        @Test @DisplayName("STOPPED decrements currentLevel by 1")
        void stopped_decrementsLevel() {
            StudentProgress p = progressAt(5);
            service.applyStatusToProgress(p, SpaceshipStatus.STOPPED);
            assertEquals(4, p.getCurrentLevel());
            assertEquals(0, p.getCurrentProgress());
        }

        @Test @DisplayName("STOPPED at level 1 stays at level 1 (floor guard)")
        void stopped_atLevelOne_staysAtOne() {
            StudentProgress p = progressAt(1);
            service.applyStatusToProgress(p, SpaceshipStatus.STOPPED);
            assertEquals(1, p.getCurrentLevel());
        }

        @Test @DisplayName("MOVING increments progress counter without changing level")
        void moving_incrementsProgressCounter() {
            StudentProgress p = progressAt(3);
            p.setCurrentProgress(7);
            service.applyStatusToProgress(p, SpaceshipStatus.MOVING);
            assertEquals(3, p.getCurrentLevel());
            assertEquals(8, p.getCurrentProgress());
        }

        @Test @DisplayName("MOVING with null progress counter treats it as 0")
        void moving_withNullProgressCounter_startsFromZero() {
            StudentProgress p = progressAt(2);
            p.setCurrentProgress(null);
            service.applyStatusToProgress(p, SpaceshipStatus.MOVING);
            assertEquals(1, p.getCurrentProgress());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("detectWeakness()")
    class DetectWeakness {

        @Test @DisplayName("Returns null for empty attempt list")
        void emptyList_returnsNull() {
            assertNull(service.detectWeakness(Collections.emptyList()));
        }

        @Test @DisplayName("Returns null when no type has enough samples (<4)")
        void belowMinSample_returnsNull() {
            assertNull(service.detectWeakness(List.of(
                    attempt("CARRYING_ADDITION", false),
                    attempt("CARRYING_ADDITION", false),
                    attempt("CARRYING_ADDITION", false))));
        }

        @Test @DisplayName("Returns null when error rate is exactly at the threshold")
        void errorRateAtThreshold_returnsNull() {
            assertNull(service.detectWeakness(List.of(
                    attempt("SIMPLE_ADDITION", false),
                    attempt("SIMPLE_ADDITION", false),
                    attempt("SIMPLE_ADDITION", true),
                    attempt("SIMPLE_ADDITION", true))));
        }

        @Test @DisplayName("Returns the type with error rate strictly above 50% and ≥4 samples")
        void highErrorRate_returnsWeaknessType() {
            assertEquals("CARRYING_ADDITION", service.detectWeakness(List.of(
                    attempt("CARRYING_ADDITION", false),
                    attempt("CARRYING_ADDITION", false),
                    attempt("CARRYING_ADDITION", false),
                    attempt("CARRYING_ADDITION", false),
                    attempt("CARRYING_ADDITION", true))));
        }

        @Test @DisplayName("Returns the HIGHEST error-rate type when multiple types have weakness")
        void multipleWeakTypes_returnsHighestErrorRate() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            attempts.addAll(nWrong(3, "SIMPLE_SUBTRACTION"));
            attempts.add(attempt("SIMPLE_SUBTRACTION", true));
            attempts.addAll(nWrong(4, "CARRYING_ADDITION"));
            assertEquals("CARRYING_ADDITION", service.detectWeakness(attempts));
        }

        @Test @DisplayName("Ignores attempts with null question type")
        void nullQuestionType_isIgnored() {
            assertNull(service.detectWeakness(List.of(
                    attempt(null, false), attempt(null, false),
                    attempt(null, false), attempt(null, false))));
        }

        @Test @DisplayName("Returns null when all types have low error rates")
        void lowErrorRates_returnsNull() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            for (int i = 0; i < 4; i++) attempts.add(attempt("SIMPLE_ADDITION",    true));
            for (int i = 0; i < 4; i++) attempts.add(attempt("SIMPLE_SUBTRACTION", true));
            assertNull(service.detectWeakness(attempts));
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("submitAnswer()")
    class SubmitAnswer {

        @Test @DisplayName("New student gets progress at level 1 and a correct answer is SOLVED")
        void newStudent_createsProgressAtLevelOne() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any(StudentProgress.class))).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(1L);

            ProgressStatusResponse r = service.submitAnswer(USER_ID, submitRequest(true));
            assertEquals(1, r.getCurrentLevel());
            assertTrue(r.isAnswerCorrect());
            assertEquals("SOLVED", r.getConcluded());
            assertTrue(r.isSuccess());
        }

        @Test @DisplayName("Reaching the level-up threshold levels up and triggers a bonus")
        void reachingThreshold_levelsUpAndTriggersBonus() {
            StudentProgress p = progressAt(2);
            p.setCurrentProgress(LevelManagerService.LEVEL_UP_THRESHOLD - 1); // one solve from leveling up
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(20L);

            ProgressStatusResponse r = service.submitAnswer(USER_ID, submitRequest(true));
            assertTrue(r.isLevelUp());
            assertTrue(r.isBonusQuestionTriggered(), "bonus must trigger on level up");
            assertEquals(3, r.getCurrentLevel());
            assertEquals("SOLVED", r.getConcluded());
        }

        @Test @DisplayName("Third wrong attempt concludes the question as FAILED")
        void thirdWrongAttempt_concludesFailed() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(5L);

            SubmitAnswerRequest req = submitRequest(false);
            req.setAttemptNumber(LevelManagerService.MAX_ATTEMPTS_PER_QUESTION); // 3rd and final try
            ProgressStatusResponse r = service.submitAnswer(USER_ID, req);

            assertFalse(r.isAnswerCorrect());
            assertEquals("FAILED", r.getConcluded());
            assertFalse(r.isBonusQuestionTriggered());
        }

        @Test @DisplayName("Three FAILED questions in a row drop the student into the sub-level")
        void threeFailsInARow_entersSubLevel() {
            StudentProgress p = progressAt(3);
            p.setConsecutiveFails(LevelManagerService.SUB_LEVEL_ENTER_FAILS - 1); // already failed twice
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(9L);

            SubmitAnswerRequest req = submitRequest(false);
            req.setAttemptNumber(LevelManagerService.MAX_ATTEMPTS_PER_QUESTION);
            ProgressStatusResponse r = service.submitAnswer(USER_ID, req);

            assertEquals("FAILED", r.getConcluded());
            assertTrue(r.isInSubLevel(), "the third consecutive fail enters the sub-level");
        }

        @Test @DisplayName("STOPPED at level 1 keeps level at 1 (floor guard)")
        void stopped_atLevelOne_levelRemainsOne() {
            StudentProgress p = progressAt(1);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(10L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nWrong(10, "SIMPLE_ADDITION"));

            assertEquals(1, service.submitAnswer(USER_ID, submitRequest(false)).getCurrentLevel());
        }

        @Test @DisplayName("A wrong answer with attempts remaining is not concluded")
        void wrongAnswerWithAttemptsLeft_notConcluded() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(2L);

            SubmitAnswerRequest req = submitRequest(false);
            req.setAttemptNumber(1); // first try — retries remain
            ProgressStatusResponse r = service.submitAnswer(USER_ID, req);

            assertFalse(r.isAnswerCorrect());
            assertNull(r.getConcluded(), "must not be concluded while retries remain");
        }

        @Test @DisplayName("Throws 404 when user is not found")
        void unknownUser_throws404() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class, () -> service.submitAnswer(999L, submitRequest(true)));
        }

        @Test @DisplayName("Error pattern is calculated and saved on wrong answer (CONFUSED_SUB_WITH_ADD)")
        void wrongAnswer_calculatesErrorPattern() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            // Question has expression "10 - 3", correct answer "7", userAnswer "13"
            Question q = new Question(testSubSubject, "10 - 3", "7", List.of(), null, "he", 2, QuestionStatus.CURRENT);
            q.setId(99L);
            when(questionRepository.findById(99L)).thenReturn(Optional.of(q));

            SubmitAnswerRequest req = new SubmitAnswerRequest(SUB_SUBJECT_ID, 99L, "13", "SIMPLE_SUBTRACTION", 2);
            service.submitAnswer(USER_ID, req);

            ArgumentCaptor<ExerciseAttempt> captor = ArgumentCaptor.forClass(ExerciseAttempt.class);
            verify(attemptRepository).save(captor.capture());
            assertEquals("CONFUSED_SUB_WITH_ADD", captor.getValue().getErrorPattern());
        }

        @Test @DisplayName("Throws 404 when sub-subject is not found")
        void unknownSubSubject_throws404() {
            SubmitAnswerRequest req = new SubmitAnswerRequest(999L, 99L, "15", "ADD", 1);
            when(subSubjectRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class, () -> service.submitAnswer(USER_ID, req));
        }

        @Test @DisplayName("Attempt is always persisted regardless of outcome")
        void attempt_alwaysPersisted() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            service.submitAnswer(USER_ID, submitRequest(false));
            verify(attemptRepository, times(1)).save(any(ExerciseAttempt.class));
        }

        @Test @DisplayName("totalStars is reflected in the response from User entity")
        void totalStars_reflectedInResponse() {
            when(testUser.getTotalStars()).thenReturn(150);
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            assertEquals(150, service.submitAnswer(USER_ID, submitRequest(true)).getTotalStars());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test @DisplayName("Returns a default snapshot (level 1) when no progress record exists")
        void noProgress_returnsDefault() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());

            ProgressStatusResponse r = service.getStatus(USER_ID, SUB_SUBJECT_ID);
            assertEquals(1, r.getCurrentLevel());
            assertEquals(0, r.getTotalAttempts());
            assertFalse(r.isInSubLevel());
            assertEquals(LevelManagerService.LEVEL_UP_THRESHOLD, r.getLevelProgressTarget());
            assertTrue(r.isSuccess());
        }

        @Test @DisplayName("Returns the stored level even if the progress record is inactive")
        void inactiveProgress_returnsStoredLevel() {
            StudentProgress inactive = progressAt(5);
            inactive.setActive(false);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(inactive));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(0L);

            assertEquals(5, service.getStatus(USER_ID, SUB_SUBJECT_ID).getCurrentLevel());
        }

        @Test @DisplayName("Returns correct currentLevel when active progress record exists")
        void activeProgress_returnsCurrentLevel() {
            StudentProgress p = progressAt(7);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            ProgressStatusResponse r = service.getStatus(USER_ID, SUB_SUBJECT_ID);
            assertEquals(7, r.getCurrentLevel());
            assertTrue(r.isSuccess());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getNextQuestion()")
    class GetNextQuestion {

        @Test @DisplayName("Calls cluster-aware generator with (SubSubject, ssl, diff, lang, mc, cluster)")
        void callsGeneratorWithCorrectSignature() {
            StudentProgress p = progressAt(5);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class)))
                    .thenReturn(stubQuestion(5));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(5));

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            // subSubjectLevel = 5 (raw); difficulty now scales on the 1–10 scale (= level 5).
            // mc is a coin flip at difficulty ≥ 3, so don't assert a fixed value here.
            verify(calculationGenerator).createQuestion(
                    any(SubSubject.class), eq(5), eq(5), eq("he"), anyBoolean(), any(ClusterContext.class));
        }

        @Test @DisplayName("New student gets a question at level 1")
        void newStudent_getsLevelOneQuestion() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class)))
                    .thenReturn(stubQuestion(1));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(1));

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(any(), eq(1), eq(1), anyString(), anyBoolean(), any(ClusterContext.class));
            assertEquals(1, r.getDifficultyLevel());
        }

        @Test @DisplayName("Archives the question immediately after it is shown to the student")
        void getNextQuestion_archivesQuestion() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class)))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(archiveRepository, times(1)).save(any(QuestionArchive.class));
        }

        @Test @DisplayName("Active question NOT excluded → resumed (regular refresh keeps the same question)")
        void activeQuestion_resumedWhenNotExcluded() {
            StudentProgress p = progressAt(3);
            p.setActiveQuestionId(555L);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            Question active = new Question(testSubSubject, "8 / 4", "2", List.of("8 / 4 = 2"), null, "he", 3, QuestionStatus.CURRENT);
            active.setId(555L);
            when(questionRepository.findById(555L)).thenReturn(Optional.of(active));

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            assertEquals(555L, r.getQuestionId());
            verify(calculationGenerator, never())
                    .createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class));
        }

        @Test @DisplayName("Active question IN excludeQuestionIds → dropped, fresh one generated (Daily/Recommended separation)")
        void excludedActiveQuestion_regeneratesInstead() {
            StudentProgress p = progressAt(3);
            p.setActiveQuestionId(555L); // would normally resume — but it's excluded below
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class)))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false, List.of(555L));

            assertNotEquals(555L, r.getQuestionId());
            verify(calculationGenerator)
                    .createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class));
        }

        @Test @DisplayName("QuestionResponse contains expression, correctAnswer, difficultyLevel, and questionId")
        void response_containsGeneratedQuestionData() {
            StudentProgress p = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class)))
                    .thenReturn(stubQuestion(4));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(4));

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);
            assertEquals(99L, r.getQuestionId());
            assertEquals("10 + 5", r.getExpression());
            assertEquals("15", r.getCorrectAnswer());
            assertEquals(4, r.getDifficultyLevel());
            assertEquals(SUB_SUBJECT_ID, r.getSubSubjectId());
            assertFalse(r.isBonus());
            assertEquals(0, r.getStarsReward());
        }

        @Test @DisplayName("Solution List<String> steps are joined into a newline-delimited String")
        void solution_listIsJoinedToString() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            Question multiStep = new Question(testSubSubject, "5 + 7 * 3", "26",
                    List.of("7 * 3 = 21", "5 + 21 = 26"), null, "he", 2, QuestionStatus.CURRENT);
            multiStep.setId(77L);
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean(), any(ClusterContext.class)))
                    .thenReturn(multiStep);
            when(questionRepository.save(any())).thenReturn(multiStep);

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);
            assertEquals("7 * 3 = 21\n5 + 21 = 26", r.getSolution());
        }

        @Test @DisplayName("Multiple-choice flag is derived and forwarded to the generator")
        void multipleChoiceFlag_forwardedToGenerator() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), eq(true), any(ClusterContext.class)))
                    .thenReturn(stubQuestion(2));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(2));

            // difficulty 2 ≤ MC band, so the generator is asked for multiple-choice.
            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "en", true);
            verify(calculationGenerator).createQuestion(any(), anyInt(), anyInt(), eq("en"), eq(true), any(ClusterContext.class));
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getBonusQuestion()")
    class BonusQuestion {

        @Test @DisplayName("Returns BonusQuestionResponse with bonus=true and starsReward=50")
        void bonusQuestion_returnsCorrectFlags() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            BonusQuestionResponse r = service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            assertTrue(r.isBonus());
            assertEquals(BonusQuestionResponse.BONUS_STARS, r.getStarsReward());
            assertTrue(r.isSuccess());
        }

        @Test @DisplayName("Always uses the current sub-subject for generation")
        void bonusQuestion_usesCurrentSubSubject() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            verify(calculationGenerator).createQuestion(eq(testSubSubject), anyInt(), anyInt(), anyString(), anyBoolean());
        }

        @Test @DisplayName("Bonus difficulty is one band above the student's current level")
        void bonusDifficulty_isOneAboveCurrentLevel() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(4));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(4));

            service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            // currentLevel = 3 → bonus difficulty = min(3 + 1, 10) = 4; subSubjectLevel stays = currentLevel.
            verify(calculationGenerator).createQuestion(any(), eq(3), eq(4), anyString(), anyBoolean());
        }

        @Test @DisplayName("Bonus question is always generated as multiple-choice")
        void bonusQuestion_isAlwaysMultipleChoice() {
            StudentProgress p = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            verify(calculationGenerator).createQuestion(any(), anyInt(), anyInt(), anyString(), eq(true));
        }

        @Test @DisplayName("Archives the bonus question so it won't be repeated next time")
        void bonusQuestion_archivesAfterGeneration() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            verify(archiveRepository, times(1)).save(any(QuestionArchive.class));
        }

        @Test @DisplayName("Dedup: skips a seen expression and picks the second distinct candidate")
        void dedup_skipsSeen_picksFirstFreshExpression() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));

            // Archive already has the expression "10 + 5" seen
            when(archiveRepository.findSeenExpressionsByUserAndSubSubject(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Set.of("10 + 5"));

            Question seen  = stubQuestion(3);  // expression = "10 + 5"
            Question fresh = new Question(testSubSubject, "3 * 7 + 2", "23",
                    List.of("3 * 7 = 21", "21 + 2 = 23"), null, "he", 3, QuestionStatus.CURRENT);
            fresh.setId(100L);


            // First call returns seen expression, second returns fresh
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(seen, fresh);
            when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BonusQuestionResponse r = service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");

            assertEquals("3 * 7 + 2", r.getExpression(),
                    "Should return the fresh expression, not the previously-seen one");
        }

        @Test @DisplayName("Dedup: accepts repeat if all attempts produce seen expressions")
        void dedup_acceptsRepeat_whenPoolExhausted() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(archiveRepository.findSeenExpressionsByUserAndSubSubject(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Set.of("10 + 5")); // all attempts will return "10 + 5"

            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3)); // always returns the seen expression
            when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BonusQuestionResponse r = service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            assertNotNull(r, "Must return a result even when no fresh expression is found");
            assertTrue(r.isBonus());
        }
    }


    @Nested
    @DisplayName("submitBonusAnswer()")
    class SubmitBonusAnswer {

        // A bonus question the student is answering; correctAnswer "42".
        private Question bonusQuestion() {
            Question q = new Question(testSubSubject, "6 * 7", "42", List.of(), null, "he", 3, QuestionStatus.CURRENT);
            q.setId(77L);
            when(questionRepository.findById(77L)).thenReturn(Optional.of(q));
            return q;
        }

        private SubmitAnswerRequest bonusRequest(String userAnswer) {
            return new SubmitAnswerRequest(SUB_SUBJECT_ID, 77L, userAnswer, "SIMPLE_MULTIPLICATION", 3);
        }

        @Test @DisplayName("Correct bonus answer awards exactly 50 stars and persists the user")
        void correctBonusAnswer_awards50Stars() {
            User spyUser = mock(User.class);
            when(spyUser.getId()).thenReturn(USER_ID);
            when(spyUser.getTotalStars()).thenReturn(100);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(spyUser));
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            bonusQuestion();

            service.submitBonusAnswer(USER_ID, bonusRequest("42"));
            verify(spyUser).setTotalStars(150);
            verify(userRepository).save(spyUser);
        }

        @Test @DisplayName("Incorrect bonus answer awards zero stars and does NOT save the user")
        void incorrectBonusAnswer_awardsNoStars() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            bonusQuestion();

            service.submitBonusAnswer(USER_ID, bonusRequest("99"));
            verify(testUser, never()).setTotalStars(anyInt());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test @DisplayName("A bonus answer is recorded as an exercise attempt (feeds the ML pipeline)")
        void bonusAnswer_recordsAttempt() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            bonusQuestion();

            service.submitBonusAnswer(USER_ID, bonusRequest("99"));

            ArgumentCaptor<ExerciseAttempt> captor = ArgumentCaptor.forClass(ExerciseAttempt.class);
            verify(attemptRepository).save(captor.capture());
            ExerciseAttempt saved = captor.getValue();
            assertEquals(77L, saved.getQuestionId());
            assertEquals("99", saved.getUserAnswer());
            assertFalse(saved.getIsCorrect());
            assertNotNull(saved.getErrorPattern());
        }

        @Test @DisplayName("Response totalStars reflects the updated value after a correct answer")
        void correctBonusAnswer_responseContainsUpdatedStars() {
            User realUser = mock(User.class);
            when(realUser.getId()).thenReturn(USER_ID);
            when(realUser.getTotalStars()).thenReturn(200).thenReturn(250);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(realUser));
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            bonusQuestion();

            ProgressStatusResponse r = service.submitBonusAnswer(USER_ID, bonusRequest("42"));
            assertTrue(r.isSuccess());
            assertTrue(r.getTotalStars() >= 200);
        }

        @Test @DisplayName("submitBonusAnswer returns a valid ProgressStatusResponse")
        void submitBonusAnswer_returnsProgressStatusResponse() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            bonusQuestion();

            ProgressStatusResponse r = service.submitBonusAnswer(USER_ID, bonusRequest("99"));
            assertNotNull(r);
            assertTrue(r.isSuccess());
        }
    }


    @Nested
    @DisplayName("Adaptive progression scenario")
    class ProgressionScenario {

        @Test @DisplayName("Full cycle: level up → fail into sub-level → recover out of it")
        void fullProgressionCycle() {
            StudentProgress p = progressAt(2);
            p.setCurrentProgress(LevelManagerService.LEVEL_UP_THRESHOLD - 1);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(12L);

            // 1) One more correct answer crosses the threshold → level up to 3
            ProgressStatusResponse up = service.submitAnswer(USER_ID, submitRequest(true));
            assertTrue(up.isLevelUp());
            assertEquals(3, up.getCurrentLevel());

            // 2) Three FAILED questions in a row → enter the sub-level
            for (int i = 0; i < LevelManagerService.SUB_LEVEL_ENTER_FAILS; i++) {
                SubmitAnswerRequest fail = submitRequest(false);
                fail.setAttemptNumber(LevelManagerService.MAX_ATTEMPTS_PER_QUESTION);
                service.submitAnswer(USER_ID, fail);
            }
            assertTrue(p.getInSubLevel(), "should be in the sub-level after 3 fails");

            // 3) Three SOLVED in the sub-level → recover back to the normal level
            for (int i = 0; i < LevelManagerService.SUB_LEVEL_EXIT_SOLVES; i++) {
                service.submitAnswer(USER_ID, submitRequest(true));
            }
            assertFalse(p.getInSubLevel(), "should leave the sub-level after 3 solves");
        }
    }



    @Nested
    @DisplayName("getInitialAssessmentQuestion()")
    class GetInitialAssessmentQuestion {

        @Test @DisplayName("Calculates correct levels based on grade and confidence")
        void calculatesCorrectLevels() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(1));
            when(questionRepository.save(any())).thenAnswer(inv -> {
                Question q = inv.getArgument(0);
                q.setId(99L);
                return q;
            });

            InitialAssessmentRequest req = new InitialAssessmentRequest(SUB_SUBJECT_ID, 2, "EASY", "he");
            QuestionResponse r = service.getInitialAssessmentQuestion(USER_ID, req);

            verify(calculationGenerator).createQuestion(testSubSubject, 1, 1, "he", true);
            verify(archiveRepository).save(any(QuestionArchive.class));
            assertEquals(99L, r.getQuestionId());
        }

        @Test @DisplayName("Updates existing progress instead of creating new if active")
        void updatesExistingProgress() {
            StudentProgress p = progressAt(5); // old level
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(2));
            when(questionRepository.save(any())).thenAnswer(inv -> {
                Question q = inv.getArgument(0);
                q.setId(99L);
                return q;
            });

            InitialAssessmentRequest req = new InitialAssessmentRequest(SUB_SUBJECT_ID, 5, "MEDIUM", "en");
            service.getInitialAssessmentQuestion(USER_ID, req);

            verify(calculationGenerator).createQuestion(testSubSubject, 2, 2, "en", true);
            assertEquals(2, p.getCurrentLevel(), "Should update the level to 2 based on grade 5");
            assertEquals(0, p.getCurrentProgress(), "Should reset current progress");
            verify(progressRepository).save(p);
        }
    }

    private StudentProgress progressAt(int level) {
        return new StudentProgress(testUser, testSubSubject, level, 0, true);
    }

    private ExerciseAttempt attempt(String questionType, boolean correct) {
        return new ExerciseAttempt(
                testUser, testSubSubject, null, correct, 1, questionType, "mockAnswer", null, LocalDateTime.now());
    }

    private List<ExerciseAttempt> nCorrect(int n, String questionType) {
        List<ExerciseAttempt> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(attempt(questionType, true));
        return list;
    }

    private List<ExerciseAttempt> nWrong(int n, String questionType) {
        List<ExerciseAttempt> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(attempt(questionType, false));
        return list;
    }

    private SubmitAnswerRequest submitRequest(boolean correct) {
        String answer = correct ? "15" : "wrong_answer";
        return new SubmitAnswerRequest(SUB_SUBJECT_ID, 99L, answer, "SIMPLE_ADDITION", 1);
    }

    private Question stubQuestion(int difficultyLevel) {
        return new Question(
                testSubSubject, "10 + 5", "15",
                List.of("Step 1: 10 + 5 = 15", "Answer: 15"),
                null, "he", difficultyLevel, QuestionStatus.CURRENT);
    }

    private Question stubSavedQuestion(int difficultyLevel) {
        Question q = stubQuestion(difficultyLevel);
        q.setId(99L);
        return q;
    }
}