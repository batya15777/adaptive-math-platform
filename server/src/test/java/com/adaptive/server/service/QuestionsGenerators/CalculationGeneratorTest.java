package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private SubSubject calculation;

    @BeforeEach
    void setUp() {
        templateRepository = mock(QuestionTemplateRepository.class);
        subSubjectRepository = mock(SubSubjectRepository.class);
        generator = new CalculationGenerator(templateRepository, subSubjectRepository);

        calculation = new SubSubject();
        calculation.setName("Calculation");
        when(subSubjectRepository.findByName("Calculation")).thenReturn(calculation);
    }

    private QuestionTemplate template(String expression, Integer difficulty) {
        QuestionTemplate t = new QuestionTemplate(calculation, expression);
        t.setDifficultyLevel(difficulty);
        return t;
    }

    @Test
    void createsQuestionFromMatchingTemplate() {
        when(templateRepository.findAllBySubSubject_Name("Calculation"))
                .thenReturn(List.of(template("X + X", 2)));

        Question question = generator.createQuestion(2, "en", false);

        assertNotNull(question);
        assertEquals("en", question.getLanguage());
        assertEquals(2, question.getDifficultyLevel());
        assertNull(question.getOptions());
        // Placeholders replaced: no 'X' left and the answer matches the expression
        assertFalse(question.getExpression().contains("X"));
        String[] parts = question.getExpression().split(" \\+ ");
        int expected = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
        assertEquals(String.valueOf(expected), question.getCorrectAnswer());
    }

    @Test
    void fallsBackToHardestTemplateWhenNoExactDifficultyMatch() {
        when(templateRepository.findAllBySubSubject_Name("Calculation"))
                .thenReturn(Arrays.asList(template("X + 1", 1), template("X * X", 3)));

        Question question = generator.createQuestion(5, "en", false);

        assertNotNull(question);
        assertTrue(question.getExpression().contains("*"), "should fall back to the hardest template");
    }

    @Test
    void generatesFourUniqueMultipleChoiceOptionsIncludingCorrectAnswer() throws Exception {
        when(templateRepository.findAllBySubSubject_Name("Calculation"))
                .thenReturn(List.of(template("X + X", 1)));

        Question question = generator.createQuestion(1, "en", true);

        assertNotNull(question.getOptions());
        List<String> options = Arrays.asList(new ObjectMapper().readValue(question.getOptions(), String[].class));
        assertEquals(4, options.size());
        assertEquals(4, options.stream().distinct().count(), "options must be unique");
        assertTrue(options.contains(question.getCorrectAnswer()), "options must contain the correct answer");
    }

    @Test
    void throwsWhenSubSubjectIsMissing() {
        when(subSubjectRepository.findByName("Calculation")).thenReturn(null);

        assertThrows(QuestionGenerationException.class,
                () -> generator.createQuestion(1, "en", false));
    }

    @Test
    void throwsWhenNoTemplatesExist() {
        when(templateRepository.findAllBySubSubject_Name("Calculation"))
                .thenReturn(Collections.emptyList());

        assertThrows(QuestionGenerationException.class,
                () -> generator.createQuestion(1, "en", false));
    }

    @Test
    void solutionFollowsOperatorPrecedence() {
        String solution = generator.generateSolution("2 + 3 * 4");

        assertEquals(
                "Step 1: 3 * 4 = 12\n" +
                "Step 2: 2 + 12 = 14\n" +
                "Answer: 14", solution);
    }

    @Test
    void solutionResolvesParenthesesFirst() {
        String solution = generator.generateSolution("(2 + 3) * 4");

        assertEquals(
                "Step 1: 2 + 3 = 5\n" +
                "Step 2: 5 * 4 = 20\n" +
                "Answer: 20", solution);
    }

    @Test
    void solutionHandlesSingleOperationAndDivision() {
        assertEquals("Step 1: 9 / 2 = 4.5\nAnswer: 4.5", generator.generateSolution("9 / 2"));
    }

    @Test
    void generatedQuestionIncludesSolutionEndingWithTheAnswer() {
        when(templateRepository.findAllBySubSubject_Name("Calculation"))
                .thenReturn(List.of(template("X + X", 1)));

        Question question = generator.createQuestion(1, "en", false);

        assertNotNull(question.getSolution());
        assertTrue(question.getSolution().startsWith("Step 1: "));
        assertTrue(question.getSolution().endsWith("Answer: " + question.getCorrectAnswer()));
    }

    @Test
    void distractorsScaleWithLargeAnswers() {
        String json = generator.generateMultipleChoiceOptions(1200.0);
        assertTrue(json.startsWith("[") && json.endsWith("]"));
        // With a 15% spread, at least one distractor should differ from 1200 by more than 5
        assertNotEquals("[\"1200\",\"1200\",\"1200\",\"1200\"]", json);
    }
}
