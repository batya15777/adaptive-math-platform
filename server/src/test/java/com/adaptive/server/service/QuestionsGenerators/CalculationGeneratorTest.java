package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalculationGeneratorTest {

    private QuestionTemplateRepository templateRepository;
    private SubSubjectRepository subSubjectRepository;
    private CalculationGenerator generator;
    private SubSubject addSubSubject;

    @BeforeEach
    void setUp() {
        templateRepository = mock(QuestionTemplateRepository.class);
        subSubjectRepository = mock(SubSubjectRepository.class);
        generator = new CalculationGenerator(templateRepository, subSubjectRepository);

        addSubSubject = subSubject("add");
        when(subSubjectRepository.findByNameAndSubject_Name("add", "Calculation")).thenReturn(addSubSubject);
    }

    private SubSubject subSubject(String name) {
        return new SubSubject(name, new Subject());
    }

    private QuestionTemplate template(SubSubject subSubject, String expression, Integer difficulty) {
        QuestionTemplate t = new QuestionTemplate(subSubject, expression);
        t.setDifficultyLevel(difficulty);
        return t;
    }

    @Test
    void createsQuestionFromMatchingTemplate() {
        when(templateRepository.findAllBySubSubject_Name("add"))
                .thenReturn(List.of(template(addSubSubject, "X + X", 2)));

        Question question = generator.createQuestion(CalculationOperation.ADD, 2, "en", false);

        assertNotNull(question);
        assertEquals("en", question.getLanguage());
        assertEquals(2, question.getDifficultyLevel());
        assertTrue(question.getOptions().isEmpty());
        assertFalse(question.getExpression().contains("X"));
        String[] parts = question.getExpression().split(" \\+ ");
        int expected = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
        assertEquals(String.valueOf(expected), question.getCorrectAnswer());
    }

    @Test
    void fallsBackToHardestTemplateWhenNoExactDifficultyMatch() {
        when(templateRepository.findAllBySubSubject_Name("add"))
                .thenReturn(Arrays.asList(
                        template(addSubSubject, "X + 1", 1),
                        template(addSubSubject, "X + X + X", 3)));

        Question question = generator.createQuestion(CalculationOperation.ADD, 5, "en", false);

        assertNotNull(question);
        assertEquals(2, question.getExpression().chars().filter(c -> c == '+').count(),
                "should fall back to the hardest template");
    }

    @Test
    void usesOperationDefaultTemplateWhenNoTemplatesExist() {
        SubSubject mult = subSubject("mult");
        when(subSubjectRepository.findByNameAndSubject_Name("mult", "Calculation")).thenReturn(mult);
        when(templateRepository.findAllBySubSubject_Name("mult")).thenReturn(Collections.emptyList());

        Question question = generator.createQuestion(CalculationOperation.MULT, 1, "en", false);

        assertNotNull(question);
        assertTrue(question.getExpression().contains("*"), "default template should use the operation symbol");
        String[] parts = question.getExpression().split(" \\* ");
        int expected = Integer.parseInt(parts[0]) * Integer.parseInt(parts[1]);
        assertEquals(String.valueOf(expected), question.getCorrectAnswer());
    }

    @Test
    void divisionQuestionsPreferWholeAnswers() {
        SubSubject div = subSubject("div");
        when(subSubjectRepository.findByNameAndSubject_Name("div", "Calculation")).thenReturn(div);
        when(templateRepository.findAllBySubSubject_Name("div")).thenReturn(Collections.emptyList());

        Question question = generator.createQuestion(CalculationOperation.DIV, 1, "en", false);

        assertFalse(question.getCorrectAnswer().contains("."),
                "division answer should be a whole number, got: "
                        + question.getExpression() + " = " + question.getCorrectAnswer());
    }

    @Test
    void generatesFourUniqueMultipleChoiceOptionsIncludingCorrectAnswer() {
        when(templateRepository.findAllBySubSubject_Name("add"))
                .thenReturn(List.of(template(addSubSubject, "X + X", 1)));

        Question question = generator.createQuestion(CalculationOperation.ADD, 1, "en", true);

        List<String> options = question.getOptions();
        assertEquals(4, options.size());
        assertEquals(4, options.stream().distinct().count(), "options must be unique");
        assertTrue(options.contains(question.getCorrectAnswer()), "options must contain the correct answer");
    }

    @Test
    void throwsWhenSubSubjectIsMissing() {
        when(subSubjectRepository.findByNameAndSubject_Name("sub", "Calculation")).thenReturn(null);

        assertThrows(QuestionGenerationException.class,
                () -> generator.createQuestion(CalculationOperation.SUB, 1, "en", false));
    }

    @Test
    void operationParsesFromStringCaseInsensitively() {
        assertEquals(CalculationOperation.ADD, CalculationOperation.from("add"));
        assertEquals(CalculationOperation.MULT, CalculationOperation.from("MULT"));
        assertEquals(CalculationOperation.DIV, CalculationOperation.from("Div"));
        assertEquals(CalculationOperation.MIXED, CalculationOperation.from("mixed"));
        assertThrows(IllegalArgumentException.class, () -> CalculationOperation.from("pow"));
    }

    @Test
    void mixedQuestionCombinesOperationsAndSolvesWithPrecedence() {
        SubSubject mixed = subSubject("mixed");
        when(subSubjectRepository.findByNameAndSubject_Name("mixed", "Calculation")).thenReturn(mixed);
        when(templateRepository.findAllBySubSubject_Name("mixed")).thenReturn(Collections.emptyList());

        Question question = generator.createQuestion(CalculationOperation.MIXED, 1, "en", false);

        assertTrue(question.getExpression().contains("+"));
        assertTrue(question.getExpression().contains("*"));
        // First step must be the multiplication (precedence), not the addition
        assertTrue(question.getSolution().get(0).contains("*"),
                "multiplication should be solved first, steps: " + question.getSolution());
        assertEquals("Answer: " + question.getCorrectAnswer(), question.getSolution().getLast());
    }

    @Test
    void mixedTemplateWithDivisionPrefersWholeAnswers() {
        SubSubject mixed = subSubject("mixed");
        when(subSubjectRepository.findByNameAndSubject_Name("mixed", "Calculation")).thenReturn(mixed);
        when(templateRepository.findAllBySubSubject_Name("mixed"))
                .thenReturn(List.of(template(mixed, "X + X / X", 1)));

        Question question = generator.createQuestion(CalculationOperation.MIXED, 1, "en", false);

        assertFalse(question.getCorrectAnswer().contains("."),
                "mixed answer with division should be whole, got: "
                        + question.getExpression() + " = " + question.getCorrectAnswer());
    }

    @Test
    void solutionFollowsOperatorPrecedence() {
        List<String> solution = generator.generateSolution("2 + 3 * 4");

        assertEquals(List.of("3 * 4 = 12", "2 + 12 = 14", "Answer: 14"), solution);
    }

    @Test
    void solutionResolvesParenthesesFirst() {
        List<String> solution = generator.generateSolution("(2 + 3) * 4");

        assertEquals(List.of("2 + 3 = 5", "5 * 4 = 20", "Answer: 20"), solution);
    }

    @Test
    void solutionHandlesSingleOperationAndDivision() {
        assertEquals(List.of("9 / 2 = 4.5", "Answer: 4.5"), generator.generateSolution("9 / 2"));
    }

    @Test
    void generatedQuestionIncludesSolutionEndingWithTheAnswer() {
        when(templateRepository.findAllBySubSubject_Name("add"))
                .thenReturn(List.of(template(addSubSubject, "X + X", 1)));

        Question question = generator.createQuestion(CalculationOperation.ADD, 1, "en", false);

        assertFalse(question.getSolution().isEmpty());
        assertEquals("Answer: " + question.getCorrectAnswer(), question.getSolution().getLast());
    }

    @Test
    void distractorsScaleWithLargeAnswers() {
        List<String> options = generator.generateMultipleChoiceOptions(1200.0);

        assertEquals(4, options.size());
        assertEquals(4, options.stream().distinct().count());
        assertTrue(options.contains("1200"));
    }
}
