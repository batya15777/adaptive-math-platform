package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CalculationGenerator extends QuestionGenerator {

    private static final String SUB_SUBJECT_NAME = "Calculation";
    private static final int OPTIONS_COUNT = 4;
    // Distractors are spread within +/- 15% of the correct answer (at least +/- 1)
    private static final double DISTRACTOR_SPREAD = 0.15;

    // Formatter to clean up whole numbers (e.g., 24.0 -> "24") but keep valid decimals (e.g., "24.5")
    private final DecimalFormat format = new DecimalFormat("0.##");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CalculationGenerator(QuestionTemplateRepository templateRepository, SubSubjectRepository subSubjectRepository) {
        super(templateRepository, subSubjectRepository);
    }

    @Override
    protected String getSubSubjectName() {
        return SUB_SUBJECT_NAME;
    }

    @Override
    public Question createQuestion(int difficultyLevel, String language, boolean multipleChoice) {
        SubSubject subSubject = resolveSubSubject();

        QuestionTemplate questionTemplate = pickTemplate(difficultyLevel, subSubject);
        String expression = fillPlaceholders(questionTemplate.getExpression(), difficultyLevel);

        Expression exp = new ExpressionBuilder(expression).build();
        double mathResult = exp.evaluate();
        String finalAnswer = format.format(mathResult);

        String optionsString = multipleChoice ? generateMultipleChoiceOptions(mathResult) : null;
        String solution = generateSolution(expression);

        return new Question(
                subSubject,
                expression,
                finalAnswer,
                solution,
                optionsString,
                language,
                difficultyLevel,
                QuestionStatus.CURRENT
        );
    }

    /**
     * Picks a random template matching the difficulty; falls back to the hardest available one.
     */
    private QuestionTemplate pickTemplate(int difficultyLevel, SubSubject subSubject) {
        List<QuestionTemplate> templates = templateRepository.findAllBySubSubject_Name(subSubject.getName());

        List<QuestionTemplate> matching = templates.stream()
                .filter(q -> q.getDifficultyLevel() != null && q.getDifficultyLevel() == difficultyLevel)
                .toList();

        if (!matching.isEmpty()) {
            return matching.get(ThreadLocalRandom.current().nextInt(matching.size()));
        }

        return templates.stream()
                .filter(q -> q.getDifficultyLevel() != null)
                .max(Comparator.comparingInt(QuestionTemplate::getDifficultyLevel))
                .orElseThrow(() -> new QuestionGenerationException(
                        "No templates with a difficulty level found for sub-subject: " + subSubject.getName()));
    }

    private String fillPlaceholders(String expression, int difficultyLevel) {
        while (expression.contains("X")) {
            expression = expression.replaceFirst("X", String.valueOf(generatePlaceholderValue(difficultyLevel)));
        }
        return expression;
    }

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern INNER_PAREN = Pattern.compile("\\(([^()]+)\\)");
    private static final Pattern MUL_DIV = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([*/])\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern ADD_SUB = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-])\\s*(-?\\d+(?:\\.\\d+)?)");

    /**
     * Builds a step-by-step solution by resolving one operation at a time:
     * innermost parentheses first, then multiplication/division, then addition/subtraction.
     */
    String generateSolution(String expression) {
        List<String> steps = new ArrayList<>();
        String current = expression.trim();

        int guard = 0;
        while (!NUMBER.matcher(current).matches()) {
            if (++guard > 100) {
                throw new QuestionGenerationException("Could not build a solution for: " + expression);
            }
            current = resolveNextStep(current, steps);
        }

        StringBuilder solution = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            solution.append("Step ").append(i + 1).append(": ").append(steps.get(i)).append("\n");
        }
        solution.append("Answer: ").append(current);
        return solution.toString();
    }

    private String resolveNextStep(String expression, List<String> steps) {
        // Work inside the innermost parentheses first
        Matcher paren = INNER_PAREN.matcher(expression);
        if (paren.find()) {
            String inner = paren.group(1).trim();
            String replacement = NUMBER.matcher(inner).matches()
                    ? inner // "(5)" -> "5", no step needed
                    : "(" + applyOneOperation(inner, steps) + ")";
            return expression.substring(0, paren.start()) + replacement + expression.substring(paren.end());
        }
        return applyOneOperation(expression, steps);
    }

    /**
     * Computes the leftmost highest-precedence operation, records it as a step,
     * and returns the expression with that operation replaced by its result.
     */
    private String applyOneOperation(String expression, List<String> steps) {
        Matcher op = MUL_DIV.matcher(expression);
        if (!op.find()) {
            op = ADD_SUB.matcher(expression);
            if (!op.find()) {
                throw new QuestionGenerationException("No operation found in: " + expression);
            }
        }

        double a = Double.parseDouble(op.group(1));
        double b = Double.parseDouble(op.group(3));
        double result = switch (op.group(2)) {
            case "*" -> a * b;
            case "/" -> a / b;
            case "+" -> a + b;
            default -> a - b;
        };

        String formatted = format.format(result);
        steps.add(format.format(a) + " " + op.group(2) + " " + format.format(b) + " = " + formatted);
        return expression.substring(0, op.start()) + formatted + expression.substring(op.end());
    }

    /**
     * Generates a JSON array string containing the correct answer and 3 close distractors,
     * scaled to the magnitude of the correct answer.
     */
    String generateMultipleChoiceOptions(double correctResult) {
        Set<String> uniqueOptions = new LinkedHashSet<>();
        uniqueOptions.add(format.format(correctResult));

        double spread = Math.max(1.0, Math.abs(correctResult) * DISTRACTOR_SPREAD);

        while (uniqueOptions.size() < OPTIONS_COUNT) {
            double offset = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * spread;
            if (Math.abs(offset) < 0.01) continue;
            uniqueOptions.add(format.format(correctResult + offset));
        }

        List<String> shuffledOptions = new ArrayList<>(uniqueOptions);
        Collections.shuffle(shuffledOptions);

        try {
            // e.g., '["14.5","11.5","15.5","9.5"]' — parsed on the frontend via JSON.parse()
            return objectMapper.writeValueAsString(shuffledOptions);
        } catch (JsonProcessingException e) {
            throw new QuestionGenerationException("Failed to serialize options: " + e.getMessage());
        }
    }
}
