package com.adaptive.server.service;

import com.adaptive.server.DTOs.SubmitAnswerRequest;
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
    private CalculationGenerator       calculationGenerator;
    private QuestionRepository         questionRepository;
    private QuestionArchiveRepository  archiveRepository;   // NEW

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
        calculationGenerator = mock(CalculationGenerator.class);
        questionRepository   = mock(QuestionRepository.class);
        archiveRepository    = mock(QuestionArchiveRepository.class);

        service = new LevelManagerService(
                attemptRepository, progressRepository,
                userRepository, subSubjectRepository,
                calculationGenerator, questionRepository,
                archiveRepository);                     // NEW arg

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

        @Test @DisplayName("Returns MOVING when total < LEVEL_UP_WINDOW")
        void fewerThan30Attempts_isMoving() {
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(29, 29, 10));
        }

        @Test @DisplayName("Returns BOOSTING when ≥30 attempts and ≥24 correct in last 30")
        void thirtyAttemptsWithHighAccuracy_isBoosting() {
            assertEquals(SpaceshipStatus.BOOSTING, service.evaluateSpaceshipStatus(30, 24, 8));
        }

        @Test @DisplayName("Returns BOOSTING only at the exact threshold of 24/30")
        void exactThreshold_isBoosting() {
            assertEquals(SpaceshipStatus.BOOSTING, service.evaluateSpaceshipStatus(30, 24, 10));
        }

        @Test @DisplayName("Returns MOVING when 30 attempts but only 23 correct")
        void justBelowBoostThreshold_isMoving() {
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(30, 23, 8));
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

        @Test @DisplayName("New student gets progress created at level 1 and returns MOVING status")
        void newStudent_createsProgressAtLevelOne() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any(StudentProgress.class))).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(1L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(List.of(attempt("SIMPLE_ADDITION", true)));

            ProgressStatusResponse r = service.submitAnswer(USER_ID, submitRequest(true));
            assertEquals(1, r.getCurrentLevel());
            assertEquals(SpaceshipStatus.MOVING.name(), r.getSpaceshipStatus());
            assertTrue(r.isSuccess());
        }

        @Test @DisplayName("30 correct answers triggers BOOSTING + bonusQuestionTriggered=true")
        void thirtyAnswers_highAccuracy_reportsLevelUp() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(30L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(30, "SIMPLE_ADDITION"));

            ProgressStatusResponse r = service.submitAnswer(USER_ID, submitRequest(true));
            assertTrue(r.isLevelUp());
            assertTrue(r.isBonusQuestionTriggered(), "bonus must be triggered on BOOSTING");
            assertEquals(SpaceshipStatus.BOOSTING.name(), r.getSpaceshipStatus());
            assertEquals(3, r.getCurrentLevel());
        }

        @Test @DisplayName("10 wrong answers returns STOPPED + inIntermediateLevel=true")
        void tenAnswers_lowAccuracy_reportsIntermediateLevel() {
            StudentProgress p = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(10L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nWrong(10, "CARRYING_ADDITION"));

            ProgressStatusResponse r = service.submitAnswer(USER_ID, submitRequest(false));
            assertTrue(r.isInIntermediateLevel());
            assertFalse(r.isBonusQuestionTriggered(), "bonus must NOT trigger on STOPPED");
            assertEquals(SpaceshipStatus.STOPPED.name(), r.getSpaceshipStatus());
            assertEquals(3, r.getCurrentLevel());
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

        @Test @DisplayName("Weakness is included in response when error rate is above threshold")
        void weakness_populatedInResponse_whenHighErrorRate() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(10L);
            List<ExerciseAttempt> attempts = new ArrayList<>(nWrong(4, "CARRYING_ADDITION"));
            attempts.addAll(nCorrect(6, "SIMPLE_ADDITION"));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(attempts);

            assertEquals("CARRYING_ADDITION", service.submitAnswer(USER_ID, submitRequest(false)).getWeaknessType());
        }

        @Test @DisplayName("Throws 404 when user is not found")
        void unknownUser_throws404() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class, () -> service.submitAnswer(999L, submitRequest(true)));
        }

        @Test @DisplayName("Throws 404 when sub-subject is not found")
        void unknownSubSubject_throws404() {
            SubmitAnswerRequest req = new SubmitAnswerRequest(999L, 1L, true, "ADD", 1);
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

        @Test @DisplayName("Returns default status (level 1, MOVING) when no progress record exists")
        void noProgress_returnsDefault() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());

            ProgressStatusResponse r = service.getStatus(USER_ID, SUB_SUBJECT_ID);
            assertEquals(1, r.getCurrentLevel());
            assertEquals(SpaceshipStatus.MOVING.name(), r.getSpaceshipStatus());
            assertEquals(0, r.getTotalAttempts());
            assertTrue(r.isSuccess());
        }

        @Test @DisplayName("Returns default status when progress record exists but is inactive")
        void inactiveProgress_returnsDefault() {
            StudentProgress inactive = progressAt(5);
            inactive.setActive(false);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(inactive));

            assertEquals(1, service.getStatus(USER_ID, SUB_SUBJECT_ID).getCurrentLevel());
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

        @Test @DisplayName("Calls generator with correct 5-arg signature (SubSubject, ssl, diff, lang, mc)")
        void callsGeneratorWithCorrectSignature() {
            StudentProgress p = progressAt(5);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(5));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(5));

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(
                    any(SubSubject.class), eq(5), eq(5), eq("he"), eq(false));
        }

        @Test @DisplayName("New student gets a question at level 1")
        void newStudent_getsLevelOneQuestion() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(1));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(1));

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(any(), eq(1), eq(1), anyString(), anyBoolean());
            assertEquals(1, r.getDifficultyLevel());
        }

        @Test @DisplayName("Archives the question immediately after it is shown to the student")
        void getNextQuestion_archivesQuestion() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(archiveRepository, times(1)).save(any(QuestionArchive.class));
        }

        @Test @DisplayName("QuestionResponse contains expression, correctAnswer, difficultyLevel, and questionId")
        void response_containsGeneratedQuestionData() {
            StudentProgress p = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
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
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(multiStep);
            when(questionRepository.save(any())).thenReturn(multiStep);

            QuestionResponse r = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);
            assertEquals("7 * 3 = 21\n5 + 21 = 26", r.getSolution());
        }

        @Test @DisplayName("Multiple-choice flag is forwarded to the generator")
        void multipleChoiceFlag_forwardedToGenerator() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), eq(true)))
                    .thenReturn(stubQuestion(2));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(2));

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "en", true);
            verify(calculationGenerator).createQuestion(any(), anyInt(), anyInt(), eq("en"), eq(true));
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
            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(any())).thenReturn(3);
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
            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(any())).thenReturn(3);
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            verify(calculationGenerator).createQuestion(eq(testSubSubject), anyInt(), anyInt(), anyString(), anyBoolean());
        }

        @Test @DisplayName("Bonus difficulty uses max available difficulty for sub-subject")
        void bonusDifficulty_usesMaxDifficulty() {
            StudentProgress p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(testSubSubject)).thenReturn(3);
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any())).thenReturn(stubSavedQuestion(3));

            service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            verify(calculationGenerator).createQuestion(any(), eq(3), eq(3), anyString(), anyBoolean());
        }

        @Test @DisplayName("Bonus question is always generated as multiple-choice")
        void bonusQuestion_isAlwaysMultipleChoice() {
            StudentProgress p = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(any())).thenReturn(3);
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
            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(any())).thenReturn(3);
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

            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(any())).thenReturn(3);

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

            when(calculationGenerator.getMaxDifficultyLevelForSubSubject(any())).thenReturn(3);
            when(calculationGenerator.createQuestion(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3)); // always returns the seen expression
            when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw; falls through gracefully
            BonusQuestionResponse r = service.getBonusQuestion(USER_ID, SUB_SUBJECT_ID, "he");
            assertNotNull(r, "Must return a result even when no fresh expression is found");
            assertTrue(r.isBonus());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("submitBonusAnswer()")
    class SubmitBonusAnswer {

        @Test @DisplayName("Correct bonus answer awards exactly 50 stars and persists the user")
        void correctBonusAnswer_awards50Stars() {
            User spyUser = mock(User.class);
            when(spyUser.getId()).thenReturn(USER_ID);
            when(spyUser.getTotalStars()).thenReturn(100);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(spyUser));
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());

            service.submitBonusAnswer(USER_ID, SUB_SUBJECT_ID, true);
            verify(spyUser).setTotalStars(150);
            verify(userRepository).save(spyUser);
        }

        @Test @DisplayName("Incorrect bonus answer awards zero stars and does NOT save the user")
        void incorrectBonusAnswer_awardsNoStars() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            service.submitBonusAnswer(USER_ID, SUB_SUBJECT_ID, false);
            verify(testUser, never()).setTotalStars(anyInt());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test @DisplayName("Response totalStars reflects the updated value after a correct answer")
        void correctBonusAnswer_responseContainsUpdatedStars() {
            User realUser = mock(User.class);
            when(realUser.getId()).thenReturn(USER_ID);
            when(realUser.getTotalStars()).thenReturn(200).thenReturn(250);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(realUser));
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());

            ProgressStatusResponse r = service.submitBonusAnswer(USER_ID, SUB_SUBJECT_ID, true);
            assertTrue(r.isSuccess());
            assertTrue(r.getTotalStars() >= 200);
        }

        @Test @DisplayName("submitBonusAnswer returns a valid ProgressStatusResponse")
        void submitBonusAnswer_returnsProgressStatusResponse() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.empty());
            ProgressStatusResponse r = service.submitBonusAnswer(USER_ID, SUB_SUBJECT_ID, false);
            assertNotNull(r);
            assertTrue(r.isSuccess());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Adaptive progression scenario")
    class ProgressionScenario {

        @Test @DisplayName("Full cycle: correct streak → level up → struggle → drop → recover")
        void fullProgressionCycle() {
            StudentProgress p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(30L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(30, "SIMPLE_ADDITION"));

            ProgressStatusResponse boost = service.submitAnswer(USER_ID, submitRequest(true));
            assertEquals(3, boost.getCurrentLevel());
            assertTrue(boost.isLevelUp());
            assertTrue(boost.isBonusQuestionTriggered());

            p = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(10L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nWrong(10, "CARRYING_ADDITION"));

            ProgressStatusResponse stop = service.submitAnswer(USER_ID, submitRequest(false));
            assertEquals(2, stop.getCurrentLevel());
            assertTrue(stop.isInIntermediateLevel());
            assertFalse(stop.isBonusQuestionTriggered());

            p = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(Optional.of(p));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID)).thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            ProgressStatusResponse moving = service.submitAnswer(USER_ID, submitRequest(true));
            assertEquals(SpaceshipStatus.MOVING.name(), moving.getSpaceshipStatus());
            assertFalse(moving.isBonusQuestionTriggered());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Test helpers
    // ─────────────────────────────────────────────────────────────────────────

    private StudentProgress progressAt(int level) {
        return new StudentProgress(testUser, testSubSubject, level, 0, true);
    }

    private ExerciseAttempt attempt(String questionType, boolean correct) {
        return new ExerciseAttempt(
                testUser, testSubSubject, null, correct, 1, questionType, LocalDateTime.now());
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
        return new SubmitAnswerRequest(SUB_SUBJECT_ID, 1L, correct, "SIMPLE_ADDITION", 1);
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