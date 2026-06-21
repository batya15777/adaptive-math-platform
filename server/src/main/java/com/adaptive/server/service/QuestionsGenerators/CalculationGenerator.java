package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CalculationGenerator extends QuestionGenerator {

    private static final String SUBJECT_NAME = "Calculation";
    private static final int    OPTIONS_COUNT = 4;

    /** Difficulty is a 1–10 scale. */
    private static final int MAX_DIFFICULTY_LEVEL = 10;

    /**
     * Difficulty 1–{@value} is the "small numbers" regime: every operand AND the final
     * result must stay below 100. Above it, numbers and operand counts grow freely.
     */
    private static final int LOW_REGIME_MAX = 4;
    private static final int LOW_REGIME_CEILING = 100;   // exclusive upper bound for the low regime

    private static final int MAX_OPERANDS = 7;           // safety cap on expression length
    private static final int MAX_FILL_ATTEMPTS = 40;     // retries to satisfy the low-regime constraint

    private final Random random = new Random();

    public CalculationGenerator(QuestionTemplateRepository templateRepository,
                                SubSubjectRepository subSubjectRepository) {
        super(templateRepository, subSubjectRepository);
    }

    @Override
    protected String getSubjectName() {
        return SUBJECT_NAME;
    }

    /**
     * Cluster-aware entry point. Applies the cohort's difficulty bias (from the ML
     * clustering) to the requested difficulty, then generates as usual.
     */
    public Question createQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                   String language, boolean multipleChoice, ClusterContext cluster) {
        int adjustedDifficulty = applyClusterBias(difficultyLevel, cluster);
        return createQuestion(subSubject, subSubjectLevel, adjustedDifficulty, language, multipleChoice);
    }

    private int applyClusterBias(int difficultyLevel, ClusterContext cluster) {
        if (cluster == null || !cluster.isAssigned() || cluster.getDifficultyBias() == 0) {
            return difficultyLevel;
        }
        int adjusted = difficultyLevel + cluster.getDifficultyBias();
        return Math.max(1, Math.min(adjusted, MAX_DIFFICULTY_LEVEL));
    }

    /**
     * Generates a single {@link Question}. Difficulty is a 1–10 scale that drives BOTH
     * the number of operands and the size of the numbers:
     * <ul>
     *   <li><strong>Difficulty 1–4</strong> — small-number regime: every operand and the
     *       final result are kept below {@value #LOW_REGIME_CEILING}.</li>
     *   <li><strong>Difficulty 5–10</strong> — operands get larger and expressions get
     *       longer; the combination with {@code subSubjectLevel} scales the magnitude.</li>
     * </ul>
     * The operator(s) come from the sub-subject's seeded templates (e.g. "add" → '+'),
     * but the expression length is generated procedurally so difficulty can exceed the
     * seeded template structures.
     *
     * @param subSubject      the sub-subject entity (add / sub / mult / div / mixed)
     * @param subSubjectLevel the student's current progression level
     * @param difficultyLevel desired difficulty, 1 (easiest) … 10 (hardest)
     * @param language        preferred language tag (e.g. {@code "he"}, {@code "en"})
     * @param multipleChoice  if {@code true}, generate 4 shuffled answer options
     */
    @Override
    public Question createQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                   String language, boolean multipleChoice) {

        int difficulty   = Math.max(1, Math.min(difficultyLevel, MAX_DIFFICULTY_LEVEL));
        List<String> operators = deriveOperators(subSubject);
        int operandCount = operandCount(difficulty, subSubjectLevel);

        String expression = null;
        double answer = 0;
        List<String> solutionSteps = null;

        // Fill numbers; in the low regime, retry until operands + result stay below 100.
        for (int attempt = 0; attempt < MAX_FILL_ATTEMPTS; attempt++) {
            List<String> tokens = buildFilledTokens(operators, operandCount, difficulty, subSubjectLevel);
            fixDivisions(tokens);                       // keep every division a multiple of 0.25
            String candidate = String.join(" ", tokens);

            List<String> steps = new ArrayList<>();
            double result = evaluate(tokens, steps);    // consumes a copy via the token list

            if (difficulty > LOW_REGIME_MAX || withinLowRegime(candidate, result)) {
                expression = candidate;
                answer = result;
                solutionSteps = steps;
                break;
            }
        }

        // Fallback: constraints somehow never satisfied — accept the last fresh attempt.
        if (expression == null) {
            List<String> tokens = buildFilledTokens(operators, operandCount, difficulty, subSubjectLevel);
            fixDivisions(tokens);
            expression = String.join(" ", tokens);
            solutionSteps = new ArrayList<>();
            answer = evaluate(tokens, solutionSteps);
        }

        String correctAnswer = fmt(answer);
        List<String> options = multipleChoice ? buildOptions(answer) : null;

        return new Question(subSubject, expression, correctAnswer,
                solutionSteps, options, language, difficulty, QuestionStatus.CURRENT);
    }

    public int getMaxDifficultyLevelForSubSubject(SubSubject subSubject) {
        Integer max = templateRepository.findMaxDifficultyLevelBySubSubject(subSubject);
        return max != null ? max : 1;
    }

    // ── Difficulty model ──────────────────────────────────────────────────────

    /**
     * Number of operands, combining difficulty (primary driver) with the student's
     * progression. Difficulty 1–2 → 2 operands, 3–4 → 3, 5–6 → 4, 7–8 → 5, 9–10 → 6,
     * with one extra term for advanced students.
     */
    private int operandCount(int difficulty, int subSubjectLevel) {
        int base = 2 + (difficulty - 1) / 2;
        if (subSubjectLevel >= 7) {
            base += 1;
        }
        return Math.min(base, MAX_OPERANDS);
    }

    /**
     * Picks a value for one operand. Numbers next to '*' stay small to avoid runaway
     * products. In the low regime numbers are bounded so the result can stay below 100;
     * above it, magnitude scales with both difficulty and the student's level.
     */
    private int operandValue(int difficulty, int operandCount, int subSubjectLevel, boolean nearMult) {
        if (nearMult) {
            int max = difficulty <= LOW_REGIME_MAX ? 5 : (4 + difficulty);
            return randomInt(2, max + 1);
        }
        int max;
        if (difficulty <= LOW_REGIME_MAX) {
            // Spread the budget across operands so an all-addition result stays under 100.
            max = Math.max(2, (LOW_REGIME_CEILING - 10) / operandCount);
        } else {
            max = 30 * (difficulty - LOW_REGIME_MAX) + 10 * subSubjectLevel;
        }
        return randomInt(1, max + 1);
    }

    /** True when every operand and the result are below {@value #LOW_REGIME_CEILING}. */
    private boolean withinLowRegime(String expression, double result) {
        if (Math.abs(result) >= LOW_REGIME_CEILING) {
            return false;
        }
        for (String tok : expression.split(" ")) {
            if (tok.matches("-?\\d+(\\.\\d+)?")
                    && Math.abs(Double.parseDouble(tok)) >= LOW_REGIME_CEILING) {
                return false;
            }
        }
        return true;
    }

    // ── Expression building ───────────────────────────────────────────────────

    /**
     * The distinct operators this sub-subject uses, read from its seeded templates
     * (e.g. "add" → ['+'], "mixed" → ['+','-','*','/']). Falls back to the operator
     * implied by the sub-subject name when no templates are seeded.
     */
    private List<String> deriveOperators(SubSubject subSubject) {
        Set<String> ops = new LinkedHashSet<>();
        for (QuestionTemplate t : templateRepository.findAllBySubSubject_Name(subSubject.getName())) {
            for (String tok : t.getExpression().split(" ")) {
                if (isOperator(tok)) {
                    ops.add(tok);
                }
            }
        }
        if (ops.isEmpty()) {
            switch (subSubject.getName().toLowerCase()) {
                case "sub":  return List.of("-");
                case "mult": return List.of("*");
                case "div":  return List.of("/");
                default:     return List.of("+");
            }
        }
        return new ArrayList<>(ops);
    }

    /** Builds a token list (operand, op, operand, …) of {@code operandCount} numbers. */
    private List<String> buildFilledTokens(List<String> operators, int operandCount,
                                           int difficulty, int subSubjectLevel) {
        List<String> ops = new ArrayList<>();
        for (int k = 0; k < operandCount - 1; k++) {
            ops.add(operators.get(random.nextInt(operators.size())));
        }

        List<String> tokens = new ArrayList<>();
        for (int idx = 0; idx < operandCount; idx++) {
            boolean nearMult = (idx > 0 && ops.get(idx - 1).equals("*"))
                    || (idx < ops.size() && ops.get(idx).equals("*"));
            tokens.add(String.valueOf(operandValue(difficulty, operandCount, subSubjectLevel, nearMult)));
            if (idx < ops.size()) {
                tokens.add(ops.get(idx));
            }
        }
        return tokens;
    }

    private boolean isOperator(String tok) {
        return tok.equals("+") || tok.equals("-") || tok.equals("*") || tok.equals("/");
    }

    // ── Evaluation ────────────────────────────────────────────────────────────

    /**
     * Evaluates the token list honoring operator precedence (* / before + -), appending
     * one solution step per operation. Mutates {@code tokens} down to the final value.
     */
    private double evaluate(List<String> tokens, List<String> solutionSteps) {
        while (tokens.contains("*") || tokens.contains("/")) {
            for (int i = 1; i < tokens.size() - 1; i++) {
                String op = tokens.get(i);
                if (op.equals("*") || op.equals("/")) {
                    double left  = Double.parseDouble(tokens.get(i - 1));
                    double right = Double.parseDouble(tokens.get(i + 1));
                    double result = op.equals("*") ? left * right : left / right;
                    solutionSteps.add(fmt(left) + " " + op + " " + fmt(right) + " = " + fmt(result));
                    tokens.set(i - 1, String.valueOf(result));
                    tokens.remove(i);
                    tokens.remove(i);
                    break;
                }
            }
        }
        while (tokens.contains("+") || tokens.contains("-")) {
            for (int i = 1; i < tokens.size() - 1; i++) {
                String op = tokens.get(i);
                if (op.equals("+") || op.equals("-")) {
                    double left  = Double.parseDouble(tokens.get(i - 1));
                    double right = Double.parseDouble(tokens.get(i + 1));
                    double result = op.equals("+") ? left + right : left - right;
                    solutionSteps.add(fmt(left) + " " + op + " " + fmt(right) + " = " + fmt(result));
                    tokens.set(i - 1, String.valueOf(result));
                    tokens.remove(i);
                    tokens.remove(i);
                    break;
                }
            }
        }
        return Double.parseDouble(tokens.get(0));
    }

    // ── Multiple choice ───────────────────────────────────────────────────────

    private List<String> buildOptions(double answer) {
        // Distractor granularity matches the answer: whole answers get whole distractors;
        // fractional answers (e.g. 2.5) get quarter-step distractors.
        double step = (answer == Math.floor(answer)) ? 1.0 : 0.25;
        Set<String> unique = new LinkedHashSet<>();
        unique.add(fmt(answer));
        for (int attempts = 0; attempts < 40 && unique.size() < OPTIONS_COUNT; attempts++) {
            int k = randomInt(1, 5); // 1..4 steps away
            double distractor = answer + k * step * (random.nextBoolean() ? 1 : -1);
            unique.add(fmt(distractor));
        }
        for (int k = 1; unique.size() < OPTIONS_COUNT; k++) {
            unique.add(fmt(answer + k * step));
        }
        List<String> options = new ArrayList<>(unique);
        Collections.shuffle(options, random);
        return options;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int randomInt(int min, int max) {
        return random.nextInt(min, max);
    }

    /**
     * Walks the token list honoring * / precedence and adjusts each '/' right operand
     * (in place) so the quotient is a multiple of 0.25.
     */
    private void fixDivisions(List<String> tokens) {
        if (tokens.isEmpty()) return;
        double current = Double.parseDouble(tokens.get(0));
        for (int i = 1; i + 1 < tokens.size(); i += 2) {
            String op = tokens.get(i);
            double right = Double.parseDouble(tokens.get(i + 1));
            switch (op) {
                case "*":
                    current = current * right;
                    break;
                case "/":
                    if (current != 0 && !isNiceQuarter(current / right)) {
                        int fixed = niceDivisor(current);
                        tokens.set(i + 1, String.valueOf(fixed));
                        right = fixed;
                    }
                    current = current / right;
                    break;
                default: // + or - : a new term starts
                    current = right;
                    break;
            }
        }
    }

    /** True when v is a multiple of 0.25 (…, 0.25, 0.5, 0.75, 1.0, …). */
    private boolean isNiceQuarter(double v) {
        double q = v * 4;
        return Math.abs(q - Math.rint(q)) < 1e-9;
    }

    /**
     * A divisor d of 4·current (so current/d is a multiple of 0.25), preferring small
     * divisors (≤ 12) to keep the numbers readable.
     */
    private int niceDivisor(double current) {
        int n = (int) Math.rint(Math.abs(current) * 4);
        if (n <= 1) return 1;
        List<Integer> small = new ArrayList<>();
        List<Integer> all   = new ArrayList<>();
        for (int d = 2; d <= n; d++) {
            if (n % d == 0) {
                all.add(d);
                if (d <= 12) small.add(d);
            }
        }
        List<Integer> pick = !small.isEmpty() ? small : all;
        return pick.isEmpty() ? 1 : pick.get(random.nextInt(pick.size()));
    }

    /** Formats a value: whole numbers without a decimal point, fractions with up to 2 dp. */
    private String fmt(double v) {
        if (v == Math.rint(v)) {
            return Long.toString((long) Math.rint(v));
        }
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
