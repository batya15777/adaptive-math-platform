package com.adaptive.server.service;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.SubSubjectAiConfig;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.QuestionArchiveRepository;
import com.adaptive.server.repository.QuestionRepository;
import com.adaptive.server.repository.StudentProgressRepository;
import com.adaptive.server.repository.SubSubjectAiConfigRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.QuestionResponse;
import com.adaptive.server.service.QuestionsGenerators.CalculationGenerator;
import com.adaptive.server.service.QuestionsGenerators.ClusterContext;
import com.adaptive.server.service.QuestionsGenerators.PolynomialGenerator;
import com.adaptive.server.service.errorpattern.ErrorPatternService;
import com.adaptive.server.service.sse.AdminSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Focused unit tests for the AI → code-based-generator fallback inside
 * {@link LevelManagerService#getNextQuestion}. This is the safety net that guarantees a
 * student always gets a question even when the AI microservice is down — it is NOT covered
 * by the existing {@code LevelManagerServiceTest}, which mocks the AI service to succeed.
 *
 * <p>Lives in its own class (rather than extending the green 57-test suite) so the existing
 * coverage stays untouched. The progress is pinned to level 2 → difficulty 2, which is
 * always multiple-choice, removing the {@code Random} coin-flip and keeping the path
 * deterministic.
 */
class QuestionGenerationFallbackTest {

    private ExerciseAttemptRepository attemptRepository;
    private StudentProgressRepository progressRepository;
    private UserRepository userRepository;
    private SubSubjectRepository subSubjectRepository;
    private SubSubjectAiConfigRepository subSubjectAiConfigRepository;
    private CalculationGenerator calculationGenerator;
    private PolynomialGenerator polynomialGenerator;
    private QuestionRepository questionRepository;
    private QuestionArchiveRepository archiveRepository;
    private ClusterContextService clusterContextService;
    private AiQuestionService aiQuestionService;
    private ErrorPatternService errorPatternService;
    private AdminSseService adminSseService;

    private LevelManagerService service;

    private User testUser;
    private SubSubject testSubSubject;

    private static final Long USER_ID = 1L;
    private static final Long SUB_SUBJECT_ID = 10L;

    @BeforeEach
    void setUp() {
        attemptRepository = mock(ExerciseAttemptRepository.class);
        progressRepository = mock(StudentProgressRepository.class);
        userRepository = mock(UserRepository.class);
        subSubjectRepository = mock(SubSubjectRepository.class);
        subSubjectAiConfigRepository = mock(SubSubjectAiConfigRepository.class);
        calculationGenerator = mock(CalculationGenerator.class);
        polynomialGenerator = mock(PolynomialGenerator.class);
        questionRepository = mock(QuestionRepository.class);
        archiveRepository = mock(QuestionArchiveRepository.class);
        clusterContextService = mock(ClusterContextService.class);
        aiQuestionService = mock(AiQuestionService.class);
        errorPatternService = mock(ErrorPatternService.class);
        adminSseService = mock(AdminSseService.class);

        service = new LevelManagerService(
                attemptRepository, progressRepository, userRepository, subSubjectRepository,
                subSubjectAiConfigRepository, calculationGenerator, polynomialGenerator,
                questionRepository, archiveRepository, clusterContextService, aiQuestionService,
                errorPatternService, adminSseService);

        // The global flag is off by default; turn it on so the AI path is attempted.
        ReflectionTestUtils.setField(service, "aiQuestionsEnabled", true);

        testUser = mock(User.class);
        when(testUser.getId()).thenReturn(USER_ID);
        when(testUser.getFullName()).thenReturn("Dalia");

        testSubSubject = mock(SubSubject.class);
        when(testSubSubject.getId()).thenReturn(SUB_SUBJECT_ID);
        when(testSubSubject.getSubject()).thenReturn(null); // not Polynomial → CalculationGenerator

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subSubjectRepository.findById(SUB_SUBJECT_ID)).thenReturn(Optional.of(testSubSubject));

        // Active progress at level 2 → difficulty 2 (≤ 2) → deterministic multiple-choice.
        StudentProgress progress = new StudentProgress(testUser, testSubSubject, 2, 0, true);
        when(progressRepository.findByUserIdAndSubSubjectId(USER_ID, SUB_SUBJECT_ID))
                .thenReturn(Optional.of(progress));

        when(clusterContextService.forUser(USER_ID)).thenReturn(ClusterContext.neutral());
        when(archiveRepository.findSeenExpressionsByUserAndSubSubject(any(), any()))
                .thenReturn(Collections.emptySet());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Question question(long id, String expression) {
        Question q = new Question(testSubSubject, expression, "15",
                List.of("Step 1", "Answer: 15"), null, "he", 2, QuestionStatus.CURRENT);
        q.setId(id);
        return q;
    }

    private QuestionResponse callNextQuestion() {
        return service.getNextQuestion(USER_ID, SUB_SUBJECT_ID, "he", false);
    }

    private void stubAiThrows() {
        when(aiQuestionService.generateQuestion(any(), any(), any(), anyInt(), any(), anyBoolean(), any(), any()))
                .thenThrow(new RuntimeException("AI service is unavailable"));
    }

    private void stubCalculationGeneratorReturns(Question q) {
        when(calculationGenerator.createQuestion(any(SubSubject.class), anyInt(), anyInt(),
                any(), anyBoolean(), any(ClusterContext.class))).thenReturn(q);
    }

    private void verifyCalculationGeneratorUsed() {
        verify(calculationGenerator).createQuestion(any(SubSubject.class), anyInt(), anyInt(),
                any(), anyBoolean(), any(ClusterContext.class));
    }

    private void verifyCalculationGeneratorNeverUsed() {
        verify(calculationGenerator, never()).createQuestion(any(SubSubject.class), anyInt(), anyInt(),
                any(), anyBoolean(), any(ClusterContext.class));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AI enabled + AI throws → falls back to the code generator and still returns a question")
    void aiEnabled_aiThrows_fallsBackToCodeGenerator() {
        when(subSubjectAiConfigRepository.findBySubSubjectId(SUB_SUBJECT_ID)).thenReturn(Optional.empty());
        stubAiThrows();
        stubCalculationGeneratorReturns(question(99L, "10 + 5"));

        QuestionResponse resp = callNextQuestion();

        assertNotNull(resp);
        assertEquals(Long.valueOf(99L), resp.getQuestionId());
        verify(aiQuestionService).generateQuestion(any(), any(), any(), anyInt(), any(), anyBoolean(), any(), any());
        verifyCalculationGeneratorUsed();
    }

    @Test
    @DisplayName("AI enabled + AI succeeds → uses the AI question, no fallback")
    void aiEnabled_aiSucceeds_usesAiResult() {
        when(subSubjectAiConfigRepository.findBySubSubjectId(SUB_SUBJECT_ID)).thenReturn(Optional.empty());
        when(aiQuestionService.generateQuestion(any(), any(), any(), anyInt(), any(), anyBoolean(), any(), any()))
                .thenReturn(question(77L, "AI generated"));

        QuestionResponse resp = callNextQuestion();

        assertEquals(Long.valueOf(77L), resp.getQuestionId());
        verifyCalculationGeneratorNeverUsed();
    }

    @Test
    @DisplayName("AI disabled → goes straight to the code generator, AI service never called")
    void aiDisabled_usesCodeGenerator() {
        ReflectionTestUtils.setField(service, "aiQuestionsEnabled", false);
        when(subSubjectAiConfigRepository.findBySubSubjectId(SUB_SUBJECT_ID)).thenReturn(Optional.empty());
        stubCalculationGeneratorReturns(question(99L, "10 + 5"));

        QuestionResponse resp = callNextQuestion();

        assertEquals(Long.valueOf(99L), resp.getQuestionId());
        verify(aiQuestionService, never())
                .generateQuestion(any(), any(), any(), anyInt(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("Forced AI-only sub-subject + AI throws → the error propagates, NO code fallback")
    void forcedAiConfig_aiThrows_propagatesWithoutFallback() {
        // Global flag off — prove it is the per-sub-subject config that forces AI.
        ReflectionTestUtils.setField(service, "aiQuestionsEnabled", false);
        SubSubjectAiConfig config = new SubSubjectAiConfig(testSubSubject, "Space topic", false);
        when(subSubjectAiConfigRepository.findBySubSubjectId(SUB_SUBJECT_ID)).thenReturn(Optional.of(config));
        stubAiThrows();

        assertThrows(RuntimeException.class, this::callNextQuestion);

        verifyCalculationGeneratorNeverUsed();
    }
}
