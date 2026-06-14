package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CalculationGenerator extends QuestionGenerator<CalculationOperation> {

    private static final String SUBJECT_NAME = "Calculation";
    private static final int OPTIONS_COUNT = 4;
    // Distractors are spread within +/- 15% of the correct answer (at least +/- 1)
    private static final double DISTRACTOR_SPREAD = 0.15;
    // How many times to re-roll a division before accepting a non-whole answer
    private static final int DIVISION_RETRIES = 50;

    // Formatter to clean up whole numbers (e.g., 24.0 -> "24") but keep valid decimals (e.g., "24.5")
    private final DecimalFormat format = new DecimalFormat("0.##");

    public CalculationGenerator(QuestionTemplateRepository templateRepository, SubSubjectRepository subSubjectRepository) {
        super(templateRepository, subSubjectRepository);
    }

    @Override
    protected String getSubjectName() {
        return SUBJECT_NAME;
    }

    @Override
    public Question createQuestion(CalculationOperation operation, int difficultyLevel, String language, boolean multipleChoice) {
        SubSubject subSubject = resolveSubSubject(operation.getSubSubjectName());

        String template = chooseTemplateExpression(operation, subSubject, difficultyLevel);
        String expression = fillPlaceholders(template, difficultyLevel);
        if (template.contains("/")) { // division involved (DIV or a mixed template)
            expression = preferWholeAnswer(template, expression, difficultyLevel);
        }

        double mathResult = evaluate(expression);
        String finalAnswer = format.format(mathResult);

        List<String> options = multipleChoice ? generateMultipleChoiceOptions(mathResult) : new ArrayList<>();
        List<String> solution = generateSolution(expression);

        return new Question(
                subSubject,
                expression,
                finalAnswer,
                solution,
                options,
                language,
                difficultyLevel,
                QuestionStatus.CURRENT
        );
    }

    /**
     * Picks a random template matching the difficulty, falling back to the hardest
     * available one. If the sub-subject has no templates at all, the operation's
     * built-in default template (e.g. "X + X") is used.
     */
    private String chooseTemplateExpression(CalculationOperation operation, SubSubject subSubject, int difficultyLevel) {
        List<QuestionTemplate> templates = templateRepository.findAllBySubSubject_Name(subSubject.getName());

        List<QuestionTemplate> matching = templates.stream()
                .filter(q -> q.getDifficultyLevel() != null && q.getDifficultyLevel() == difficultyLevel)
                .toList();

        if (!matching.isEmpty()) {
            return matching.get(ThreadLocalRandom.current().nextInt(matching.size())).getExpression();
        }

        return templates.stream()
                .filter(q -> q.getDifficultyLevel() != null)
                .max(Comparator.comparingInt(QuestionTemplate::getDifficultyLevel))
                .map(QuestionTemplate::getExpression)
                .orElse(operation.getDefaultTemplate());
    }

    private String fillPlaceholders(String expression, int difficultyLevel) {
        while (expression.contains("X")) {
            expression = expression.replaceFirst("X", String.valueOf(generatePlaceholderValue(difficultyLevel)));
        }
        return expression;
    }

    /**
     * Re-rolls the template values a few times, hoping for a whole-number result,
     * so division questions usually have clean answers. Keeps the last roll otherwise.
     */
    private String preferWholeAnswer(String template, String expression, int difficultyLevel) {
        for (int i = 0; i < DIVISION_RETRIES; i++) {
            double result = evaluate(expression);
            if (Double.isFinite(result) && result == Math.floor(result)) {
                return expression;
            }
            expression = fillPlaceholders(template, difficultyLevel);
        }
        return expression;
    }

    private double evaluate(String expression) {
        return new ExpressionBuilder(expression).build().evaluate();
    }

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern INNER_PAREN = Pattern.compile("\\(([^()]+)\\)");
    private static final Pattern MUL_DIV = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([*/])\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern ADD_SUB = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-])\\s*(-?\\d+(?:\\.\\d+)?)");

    /**
     * Builds a step-by-step solution. Returns each step as its own String,
     * e.g. ["3 * 4 = 12", "2 + 12 = 14"]. The last element is the final answer.
     */
    List<String> generateSolution(String expression) {
        List<String> steps = new ArrayList<>();
        String current = expression.trim();

        int guard = 0;
        while (!NUMBER.matcher(current).matches()) {
            if (++guard > 100) {
                throw new QuestionGenerationException("Could not build a solution for: " + expression);
            }
            current = resolveNextStep(current, steps);
        }

        steps.add("Answer: " + current);
        return steps;
    }

    private String resolveNextStep(String expression, List<String> steps) {
        Matcher paren = INNER_PAREN.matcher(expression);
        if (paren.find()) {
            String inner = paren.group(1).trim();
            String replacement = NUMBER.matcher(inner).matches()
                    ? inner
                    : "(" + applyOneOperation(inner, steps) + ")";
            return expression.substring(0, paren.start()) + replacement + expression.substring(paren.end());
        }
        return applyOneOperation(expression, steps);
    }

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
            default  -> a - b;
        };

        String formatted = format.format(result);
        steps.add(format.format(a) + " " + op.group(2) + " " + format.format(b) + " = " + formatted);
        return expression.substring(0, op.start()) + formatted + expression.substring(op.end());
    }

    /**
     * Returns the correct answer and 3 close distractors as a plain List<String>,
     * scaled to the magnitude of the correct answer.
     */
    List<String> generateMultipleChoiceOptions(double correctResult) {
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
        return shuffledOptions;
    }
}
