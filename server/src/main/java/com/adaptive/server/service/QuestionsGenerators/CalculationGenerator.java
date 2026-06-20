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
    private static final int    OPTIONS_COUNT     = 4;
    private static final double DISTRACTOR_SPREAD = 0.15;

    /**
     * Highest {@code subSubjectLevel} value that is actually seeded in the DB.
     * Students at higher levels are clamped to this value so the template query
     * always finds rows.  Update this constant when new seed bands are added.
     */
    private static final int MAX_TEMPLATE_LEVEL = 3;

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
     * clustering) to the requested difficulty, then generates as usual. This is how
     * the code-based generator becomes "cluster-aware": a struggling cohort gets
     * easier questions, a strong cohort gets harder ones — clamped to the seeded band.
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
        // Stay within the seeded template band so a template is always found.
        return Math.max(1, Math.min(adjusted, MAX_TEMPLATE_LEVEL));
    }

    /**
     * Generates a single {@link Question} by:
     * <ol>
     *   <li>Selecting a template from the DB using the <strong>3-parameter query</strong>
     *       (subSubject + difficultyLevel + subSubjectLevel).  The student's current level
     *       is clamped to {@value #MAX_TEMPLATE_LEVEL} so we always find a seeded template.
     *       A 2-parameter fallback (ignoring subSubjectLevel) guards against an un-seeded DB.
     *   <li>Replacing every {@code X} placeholder with a random integer whose range is
     *       derived from <em>both</em> {@code subSubjectLevel} and {@code difficultyLevel},
     *       so numeric difficulty scales together with structural complexity.
     *   <li>Evaluating the expression using standard operator precedence (* / before + -).
     *   <li>Optionally building 4 shuffled multiple-choice distractors.
     * </ol>
     *
     * @param subSubject      the sub-subject entity (add / sub / mult / div / mixed)
     * @param subSubjectLevel the student's current progression level (from StudentProgress)
     * @param difficultyLevel the desired structural complexity (1 = simple … 3 = complex)
     * @param language        preferred language tag (e.g. {@code "he"}, {@code "en"})
     * @param multipleChoice  if {@code true}, generate 4 shuffled answer options
     */
    @Override
    public Question createQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                   String language, boolean multipleChoice) {

        // ── Step 0: template lookup with full 3-param precision ───────────────
        // Clamp so students above level 3 still get the hardest available templates.
        int templateLevel = Math.min(subSubjectLevel, MAX_TEMPLATE_LEVEL);

        List<QuestionTemplate> templates = templateRepository
                .findAllBySubSubjectAndDifficultyLevelAndSubSubjectLevel(
                        subSubject, difficultyLevel, templateLevel);

        // Graceful fallback: if seeding is incomplete, try without the level constraint
        if (templates.isEmpty()) {
            templates = templateRepository
                    .findAllBySubSubjectAndDifficultyLevel(subSubject, difficultyLevel);
        }
        if (templates.isEmpty()) {
            throw new QuestionGenerationException(
                    "No templates found for sub-subject '" + subSubject.getName()
                    + "', difficultyLevel=" + difficultyLevel
                    + ", subSubjectLevel=" + templateLevel
                    + ". Start the server with SEED_DATA=true to initialise the template table.");
        }

        String templateExpression = templates.get(random.nextInt(templates.size())).getExpression();

        // ── Step 1: replace every X with a scaled random integer ─────────────
        List<String> tokens = new ArrayList<>(List.of(templateExpression.split(" ")));
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("X")) {
                String prevOp = (i > 0)                 ? tokens.get(i - 1) : "";
                String nextOp = (i < tokens.size() - 1) ? tokens.get(i + 1) : "";
                boolean nearMult = prevOp.equals("*") || nextOp.equals("*");

                // Numbers near '*' stay small to avoid astronomically large products.
                // Other positions scale with both subSubjectLevel and difficultyLevel
                // so the total numeric difficulty rises as the student progresses.
                // Each range keeps a real spread (min < max-1) so questions actually vary.
                int val;
                if (nearMult) {
                    val = randomInt(2, 3 + difficultyLevel * 2);             // diff1: 2–4, diff2: 2–6, diff3: 2–8
                } else {
                    int upper = (subSubjectLevel + difficultyLevel) * 5;     // lvl1/diff1: up to 10, scales up
                    val = randomInt(1, upper + 1);                           // 1 … upper (inclusive)
                }
                tokens.set(i, String.valueOf(val));
            }
        }
        // ── Step 1.5: make every division land on a multiple of 0.25 (e.g. 10 / 4 = 2.5) ──
        // Done BEFORE capturing the expression so what the student sees matches the answer.
        fixDivisions(tokens);
        String expression = String.join(" ", tokens); // the string the student sees

        List<String> solutionSteps = new ArrayList<>();

        // ── Step 2: evaluate * and / first (operator precedence), in decimals ──
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

        // ── Step 3: evaluate + and - ──────────────────────────────────────────
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

        // ── Step 4: build multiple-choice options (if requested) ──────────────
        double answer = Double.parseDouble(tokens.get(0));
        String correctAnswer = fmt(answer);
        List<String> options = null;
        if (multipleChoice) {
            // Distractor granularity matches the answer: whole answers get whole
            // distractors; fractional answers (e.g. 2.5) get quarter-step distractors.
            double step = (answer == Math.floor(answer)) ? 1.0 : 0.25;
            Set<String> unique = new LinkedHashSet<>();
            unique.add(correctAnswer);
            for (int attempts = 0; attempts < 40 && unique.size() < OPTIONS_COUNT; attempts++) {
                int k = randomInt(1, 5); // 1..4 steps away
                double distractor = answer + k * step * (random.nextBoolean() ? 1 : -1);
                unique.add(fmt(distractor));
            }
            // Guaranteed fallback so we always reach OPTIONS_COUNT
            for (int k = 1; unique.size() < OPTIONS_COUNT; k++) {
                unique.add(fmt(answer + k * step));
            }
            options = new ArrayList<>(unique);
            Collections.shuffle(options, random);
        }

        return new Question(subSubject, expression, correctAnswer,
                solutionSteps, options, language, difficultyLevel, QuestionStatus.CURRENT);
    }

    public int getMaxDifficultyLevelForSubSubject(SubSubject subSubject) {
        Integer max = templateRepository.findMaxDifficultyLevelBySubSubject(subSubject);
        return max != null ? max : 1;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int randomInt(int min, int max) {
        return random.nextInt(min, max);
    }

    /**
     * Walks the token list honoring * / precedence and adjusts each '/' right operand
     * (in place) so the quotient is a multiple of 0.25. Because seeded division templates
     * are pure '/' chains, this also keeps every intermediate result nice.
     * Editing the tokens here — before the expression is captured — keeps what the
     * student sees consistent with the computed answer.
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
