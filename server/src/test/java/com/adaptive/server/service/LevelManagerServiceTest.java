package com.adaptive.server.service;

import com.adaptive.server.DTOs.SubmitAnswerRequest;
import com.adaptive.server.entity.ExerciseAttempt;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.entity.enums.SpaceshipStatus;
import com.adaptive.server.repository.*;
import com.adaptive.server.service.QuestionsGenerators.CalculationGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import com.adaptive.server.responses.ProgressStatusResponse;
import com.adaptive.server.responses.QuestionResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    private LevelManagerService service;


    private static final Long USER_ID = 1L;
    private static final Long SUB_SUBJECT_ID = 10L;

    private User testUser;
    private SubSubject testSubSubject;

    @BeforeEach
    void setUp() {
        attemptRepository    = mock(ExerciseAttemptRepository.class);
        progressRepository   = mock(StudentProgressRepository.class);
        userRepository       = mock(UserRepository.class);
        subSubjectRepository = mock(SubSubjectRepository.class);
        calculationGenerator = mock(CalculationGenerator.class);
        questionRepository   = mock(QuestionRepository.class);

        service = new LevelManagerService(
                attemptRepository, progressRepository,
                userRepository, subSubjectRepository,
                calculationGenerator ,questionRepository);

        testUser = mock(User.class);
        when(testUser.getId()).thenReturn(USER_ID);

        testSubSubject = mock(SubSubject.class);
        when(testSubSubject.getId()).thenReturn(SUB_SUBJECT_ID);
        when(testSubSubject.getName()).thenReturn("add");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subSubjectRepository.findById(SUB_SUBJECT_ID)).thenReturn(Optional.of(testSubSubject));
    }


    @Test
    @DisplayName("Service instantiates without exceptions")
    void serviceInstantiates() {
        assertNotNull(service);
    }

    @Nested
    @DisplayName("evaluateSpaceshipStatus()")
    class EvaluateSpaceshipStatus {
        @Test
        @DisplayName("Returns MOVING when total < LEVEL_UP_WINDOW")
        void fewerThan30Attempts_isMoving() {
            SpaceshipStatus status = service.evaluateSpaceshipStatus(29, 29, 10);
            assertEquals(SpaceshipStatus.MOVING, status);
        }

        @Test
        @DisplayName("Returns BOOSTING when ≥30 attempts and ≥24 correct in last 30")
        void thirtyAttemptsWithHighAccuracy_isBoosting() {
            SpaceshipStatus status = service.evaluateSpaceshipStatus(30, 24, 8);
            assertEquals(SpaceshipStatus.BOOSTING, status);
        }

        @Test
        @DisplayName("Returns BOOSTING only at the exact threshold of 24/30")
        void exactThreshold_isBoosting() {
            assertEquals(SpaceshipStatus.BOOSTING, service.evaluateSpaceshipStatus(30, 24, 10));
        }

        @Test
        @DisplayName("Returns MOVING when 30 attempts but only 23 correct (just below boost threshold)")
        void justBelowBoostThreshold_isMoving() {
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(30, 23, 8));
        }

        @Test
        @DisplayName("Returns STOPPED when ≥10 attempts and <6 correct in last 10")
        void tenAttemptsWithLowAccuracy_isStopped() {
            SpaceshipStatus status = service.evaluateSpaceshipStatus(10, 5, 5);
            assertEquals(SpaceshipStatus.STOPPED, status);
        }

        @Test
        @DisplayName("Returns STOPPED at the boundary of 5/10 correct")
        void boundaryStop_exactlyFiveCorrect() {
            assertEquals(SpaceshipStatus.STOPPED, service.evaluateSpaceshipStatus(10, 3, 5));
        }

        @Test
        @DisplayName("Returns MOVING when 10 attempts but ≥6 correct in last 10 (not stopped)")
        void tenAttemptsWithOkAccuracy_isMoving() {
            assertEquals(SpaceshipStatus.MOVING, service.evaluateSpaceshipStatus(10, 8, 6));
        }

        @Test
        @DisplayName("BOOSTING check takes priority over STOPPED check")
        void boostingTakesPriorityOverStopped() {
            SpaceshipStatus status = service.evaluateSpaceshipStatus(30, 24, 3);
            assertEquals(SpaceshipStatus.BOOSTING, status,
                    "BOOSTING condition is evaluated before STOPPED");
        }
    }


    @Nested
    @DisplayName("applyStatusToProgress()")
    class ApplyStatusToProgress {

        @Test
        @DisplayName("BOOSTING increments currentLevel by 1 and resets progress counter")
        void boosting_incrementsLevel() {
            StudentProgress progress = progressAt(3);
            service.applyStatusToProgress(progress, SpaceshipStatus.BOOSTING);
            assertEquals(4, progress.getCurrentLevel(),    "level should go up");
            assertEquals(0, progress.getCurrentProgress(), "progress counter reset");
        }

        @Test
        @DisplayName("BOOSTING from level 1 goes to level 2")
        void boosting_fromLevelOne_goesToLevelTwo() {
            StudentProgress progress = progressAt(1);
            service.applyStatusToProgress(progress, SpaceshipStatus.BOOSTING);
            assertEquals(2, progress.getCurrentLevel());
        }

        @Test
        @DisplayName("STOPPED decrements currentLevel by 1 (intermediate drop)")
        void stopped_decrementsLevel() {
            StudentProgress progress = progressAt(5);
            service.applyStatusToProgress(progress, SpaceshipStatus.STOPPED);
            assertEquals(4, progress.getCurrentLevel(),    "level should drop by 1");
            assertEquals(0, progress.getCurrentProgress(), "progress counter reset");
        }

        @Test
        @DisplayName("STOPPED at level 1 stays at level 1 (never goes below minimum)")
        void stopped_atLevelOne_staysAtOne() {
            StudentProgress progress = progressAt(1);
            service.applyStatusToProgress(progress, SpaceshipStatus.STOPPED);
            assertEquals(1, progress.getCurrentLevel(), "level must not drop below 1");
        }

        @Test
        @DisplayName("MOVING increments currentProgress counter without changing level")
        void moving_incrementsProgressCounter() {
            StudentProgress progress = progressAt(3);
            progress.setCurrentProgress(7);
            service.applyStatusToProgress(progress, SpaceshipStatus.MOVING);
            assertEquals(3, progress.getCurrentLevel(),    "level unchanged");
            assertEquals(8, progress.getCurrentProgress(), "progress counter incremented");
        }

        @Test
        @DisplayName("MOVING with null progress counter treats it as 0")
        void moving_withNullProgressCounter_startsFromZero() {
            StudentProgress progress = progressAt(2);
            progress.setCurrentProgress(null);
            service.applyStatusToProgress(progress, SpaceshipStatus.MOVING);
            assertEquals(1, progress.getCurrentProgress());
        }
    }


    @Nested
    @DisplayName("detectWeakness()")
    class DetectWeakness {

        @Test
        @DisplayName("Returns null for empty attempt list")
        void emptyList_returnsNull() {
            assertNull(service.detectWeakness(Collections.emptyList()));
        }

        @Test
        @DisplayName("Returns null when no type has enough samples (below WEAKNESS_MIN_SAMPLE)")
        void belowMinSample_returnsNull() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            assertNull(service.detectWeakness(attempts));
        }

        @Test
        @DisplayName("Returns null when error rate is exactly at the threshold (not above)")
        void errorRateAtThreshold_returnsNull() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            attempts.add(attempt("SIMPLE_ADDITION", false));
            attempts.add(attempt("SIMPLE_ADDITION", false));
            attempts.add(attempt("SIMPLE_ADDITION", true));
            attempts.add(attempt("SIMPLE_ADDITION", true));
            assertNull(service.detectWeakness(attempts));
        }

        @Test
        @DisplayName("Returns the type with error rate strictly above 50% and ≥4 samples")
        void highErrorRate_returnsWeaknessType() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", true));
            assertEquals("CARRYING_ADDITION", service.detectWeakness(attempts));
        }

        @Test
        @DisplayName("Returns the HIGHEST error-rate type when multiple types have weakness")
        void multipleWeakTypes_returnsHighestErrorRate() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            attempts.add(attempt("SIMPLE_SUBTRACTION", false));
            attempts.add(attempt("SIMPLE_SUBTRACTION", false));
            attempts.add(attempt("SIMPLE_SUBTRACTION", false));
            attempts.add(attempt("SIMPLE_SUBTRACTION", true));
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            attempts.add(attempt("CARRYING_ADDITION", false));
            assertEquals("CARRYING_ADDITION", service.detectWeakness(attempts),
                    "Should return the type with highest error rate");
        }

        @Test
        @DisplayName("Ignores attempts with null question type")
        void nullQuestionType_isIgnored() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            attempts.add(attempt(null, false));
            attempts.add(attempt(null, false));
            attempts.add(attempt(null, false));
            attempts.add(attempt(null, false));
            assertNull(service.detectWeakness(attempts), "null types must be ignored");
        }

        @Test
        @DisplayName("Returns null when all types have low error rates")
        void lowErrorRates_returnsNull() {
            List<ExerciseAttempt> attempts = new ArrayList<>();
            for (int i = 0; i < 4; i++) attempts.add(attempt("SIMPLE_ADDITION", true));
            for (int i = 0; i < 4; i++) attempts.add(attempt("SIMPLE_SUBTRACTION", true));
            assertNull(service.detectWeakness(attempts));
        }
    }

    @Nested
    @DisplayName("submitAnswer()")
    class SubmitAnswer {

        @Test
        @DisplayName("New student gets progress created at level 1 and returns MOVING status")
        void newStudent_createsProgressAtLevelOne() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(1L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(List.of(attempt("SIMPLE_ADDITION", true)));

            ProgressStatusResponse response = service.submitAnswer(USER_ID, submitRequest(true));

            assertEquals(1,                           response.getCurrentLevel());
            assertEquals(SpaceshipStatus.MOVING.name(), response.getSpaceshipStatus());
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("After 30 attempts with 24+ correct, response has levelUp=true and BOOSTING status")
        void thirtyAnswers_highAccuracy_reportsLevelUp() {
            StudentProgress progress = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(30L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(30, "SIMPLE_ADDITION"));

            ProgressStatusResponse response = service.submitAnswer(USER_ID, submitRequest(true));

            assertTrue(response.isLevelUp(),  "levelUp flag should be true");
            assertEquals(SpaceshipStatus.BOOSTING.name(), response.getSpaceshipStatus());
            assertEquals(3, response.getCurrentLevel(), "level should have incremented");
        }

        @Test
        @DisplayName("After 10 attempts with <6 correct, response has inIntermediateLevel=true and STOPPED status")
        void tenAnswers_lowAccuracy_reportsIntermediateLevel() {
            StudentProgress progress = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(10L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nWrong(10, "CARRYING_ADDITION"));

            ProgressStatusResponse response = service.submitAnswer(USER_ID, submitRequest(false));

            assertTrue(response.isInIntermediateLevel(), "inIntermediateLevel flag should be true");
            assertEquals(SpaceshipStatus.STOPPED.name(),  response.getSpaceshipStatus());
            assertEquals(3, response.getCurrentLevel(),   "level should have dropped by 1");
        }

        @Test
        @DisplayName("STOPPED at level 1 keeps level at 1 (floor guard in DB)")
        void stopped_atLevelOne_levelRemainsOne() {
            StudentProgress progress = progressAt(1);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(10L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nWrong(10, "SIMPLE_ADDITION"));

            ProgressStatusResponse response = service.submitAnswer(USER_ID, submitRequest(false));

            assertEquals(1, response.getCurrentLevel(), "level must not go below 1");
        }

        @Test
        @DisplayName("Weakness is included in response when error rate is above threshold")
        void weakness_populatedInResponse_whenHighErrorRate() {
            StudentProgress progress = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(10L);

            List<ExerciseAttempt> attempts = new ArrayList<>(nWrong(4, "CARRYING_ADDITION"));
            attempts.addAll(nCorrect(6, "SIMPLE_ADDITION"));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(attempts);

            ProgressStatusResponse response = service.submitAnswer(USER_ID, submitRequest(false));

            assertEquals("CARRYING_ADDITION", response.getWeaknessType());
        }

        @Test
        @DisplayName("Throws 404 when user is not found")
        void unknownUser_throws404() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            SubmitAnswerRequest req = submitRequest(true);

            assertThrows(ResponseStatusException.class,
                    () -> service.submitAnswer(999L, req));
        }

        @Test
        @DisplayName("Throws 404 when sub-subject is not found")
        void unknownSubSubject_throws404() {
            SubmitAnswerRequest req = new SubmitAnswerRequest(999L, 1L, true, "ADD", 1);
            when(subSubjectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResponseStatusException.class,
                    () -> service.submitAnswer(USER_ID, req));
        }

        @Test
        @DisplayName("Attempt is always saved to the repository regardless of outcome")
        void attempt_alwaysPersisted() {
            StudentProgress progress = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            service.submitAnswer(USER_ID, submitRequest(false));

            verify(attemptRepository, times(1)).save(any(ExerciseAttempt.class));
        }
    }


    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test
        @DisplayName("Returns default status (level 1, MOVING) when no progress record exists")
        void noProgress_returnsDefault() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.empty());

            ProgressStatusResponse response = service.getStatus(USER_ID, SUB_SUBJECT_ID);

            assertEquals(1,                           response.getCurrentLevel());
            assertEquals(SpaceshipStatus.MOVING.name(), response.getSpaceshipStatus());
            assertEquals(0, response.getTotalAttempts());
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("Returns default status when progress record exists but is inactive")
        void inactiveProgress_returnsDefault() {
            StudentProgress inactive = progressAt(5);
            inactive.setActive(false);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(inactive));

            ProgressStatusResponse response = service.getStatus(USER_ID, SUB_SUBJECT_ID);

            assertEquals(1, response.getCurrentLevel(), "inactive progress must be ignored");
        }

        @Test
        @DisplayName("Returns correct currentLevel when active progress record exists")
        void activeProgress_returnsCurrentLevel() {
            StudentProgress progress = progressAt(7);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            ProgressStatusResponse response = service.getStatus(USER_ID, SUB_SUBJECT_ID);

            assertEquals(7, response.getCurrentLevel());
            assertTrue(response.isSuccess());
        }
    }


    @Nested
    @DisplayName("getNextQuestion()")
    class GetNextQuestion {

        @Test
        @DisplayName("Calls CalculationGenerator with the student's current difficulty level")
        void callsGeneratorWithCurrentLevel() {
            StudentProgress progress = progressAt(5);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(5));
            when(questionRepository.save(any(Question.class))).thenReturn(stubSavedQuestion(5)); // תוספת השמירה

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(any(), eq(5), eq("he"), eq(false));
            verify(questionRepository).save(any(Question.class));
        }

        @Test
        @DisplayName("New student gets a question at level 1")
        void newStudent_getsLevelOneQuestion() {
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(1));
            when(questionRepository.save(any(Question.class))).thenReturn(stubSavedQuestion(1)); // תוספת השמירה

            QuestionResponse response = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(any(), eq(1), anyString(), anyBoolean());
            assertNotNull(response);
            assertEquals(1, response.getDifficultyLevel());
        }

        @Test
        @DisplayName("When weakness detected, generator is called with the matching operation")
        void withWeakness_generatorUsesWeaknessOperation() {
            StudentProgress progress = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));

            List<ExerciseAttempt> attempts = new ArrayList<>(nWrong(4, "CARRYING_ADDITION"));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(attempts);
            when(calculationGenerator.createQuestion(any(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(3));
            when(questionRepository.save(any(Question.class))).thenReturn(stubSavedQuestion(3)); // תוספת השמירה

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(eq(CalculationOperation.ADD), anyInt(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Without weakness, falls back to operation derived from sub-subject name")
        void withoutWeakness_usesSubjectOperation() {
            StudentProgress progress = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(2));
            when(questionRepository.save(any(Question.class))).thenReturn(stubSavedQuestion(2)); // תוספת השמירה

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            verify(calculationGenerator).createQuestion(eq(CalculationOperation.ADD), anyInt(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("QuestionResponse contains expression, correctAnswer and difficultyLevel from generated question")
        void response_containsGeneratedQuestionData() {
            StudentProgress progress = progressAt(4);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(stubQuestion(4));
            when(questionRepository.save(any(Question.class))).thenReturn(stubSavedQuestion(4)); // תוספת השמירה

            QuestionResponse response = service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);

            assertEquals(99L,       response.getQuestionId()); // הבדיקה החדשה!
            assertEquals("10 + 5",  response.getExpression());
            assertEquals("15",      response.getCorrectAnswer());
            assertEquals(4,         response.getDifficultyLevel());
            assertEquals(SUB_SUBJECT_ID, response.getSubSubjectId());
        }

        @Test
        @DisplayName("Multiple-choice flag is forwarded to the generator")
        void multipleChoiceFlag_forwardedToGenerator() {
            StudentProgress progress = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());
            when(calculationGenerator.createQuestion(any(), anyInt(), anyString(), eq(true)))
                    .thenReturn(stubQuestion(2));
            when(questionRepository.save(any(Question.class))).thenReturn(stubSavedQuestion(2)); // תוספת השמירה

            service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "en", true);

            verify(calculationGenerator).createQuestion(any(), anyInt(), eq("en"), eq(true));
        }
    }


    @Nested
    @DisplayName("Adaptive progression scenario")
    class ProgressionScenario {

        @Test
        @DisplayName("Student progresses: correct streak → level up → struggle → level drops → recovers")
        void fullProgressionCycle() {
            StudentProgress progress = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(progressRepository.save(any(StudentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(30L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(30, "SIMPLE_ADDITION"));

            ProgressStatusResponse boost = service.submitAnswer(USER_ID, submitRequest(true));
            assertEquals(3, boost.getCurrentLevel());
            assertTrue(boost.isLevelUp());

            progress = progressAt(3);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(10L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nWrong(10, "CARRYING_ADDITION"));

            ProgressStatusResponse stop = service.submitAnswer(USER_ID, submitRequest(false));
            assertEquals(2, stop.getCurrentLevel());
            assertTrue(stop.isInIntermediateLevel());

            progress = progressAt(2);
            when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(Optional.of(progress));
            when(attemptRepository.countByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                    .thenReturn(5L);
            when(attemptRepository.findByUserIdAndSubSubjectId(eq(USER_ID), eq(SUB_SUBJECT_ID), any(Pageable.class)))
                    .thenReturn(nCorrect(5, "SIMPLE_ADDITION"));

            ProgressStatusResponse moving = service.submitAnswer(USER_ID, submitRequest(true));
            assertEquals(SpaceshipStatus.MOVING.name(), moving.getSpaceshipStatus());
            assertEquals(2, moving.getCurrentLevel());
        }
    }


    private StudentProgress progressAt(int level) {
        return new StudentProgress(testUser, testSubSubject, level, 0, true);
    }

    private ExerciseAttempt attempt(String questionType, boolean correct) {
        ExerciseAttempt a = new ExerciseAttempt(
                testUser, testSubSubject, null, correct, 1, questionType, LocalDateTime.now());
        return a;
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
        Question q = new Question(
                testSubSubject,
                "10 + 5",
                "15",
                "Step 1: 10 + 5 = 15\nAnswer: 15",
                null,
                "he",
                difficultyLevel,
                QuestionStatus.CURRENT
        );
        return q;
    }

    // הוספת פונקציית העזר לשמירת ID
    private Question stubSavedQuestion(int difficultyLevel) {
        Question q = stubQuestion(difficultyLevel);
        q.setId(99L);
        return q;
    }
}