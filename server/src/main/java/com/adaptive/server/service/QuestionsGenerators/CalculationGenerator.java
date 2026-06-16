package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import org.springframework.stereotype.Service;

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
                int val = nearMult
                        ? randomInt(2, Math.max(3, difficultyLevel * 2))
                        : randomInt(subSubjectLevel + difficultyLevel,
                                    subSubjectLevel + difficultyLevel * 2);
                tokens.set(i, String.valueOf(val));
            }
        }
        String expression = String.join(" ", tokens); // the string the student sees

        List<String> solutionSteps = new ArrayList<>();

        // ── Step 2: evaluate * and / (left-to-right, operator-precedence first) ──
        while (tokens.contains("*") || tokens.contains("/")) {
            for (int i = 1; i < tokens.size() - 1; i++) {
                String op = tokens.get(i);
                if (op.equals("*") || op.equals("/")) {
                    int left  = Integer.parseInt(tokens.get(i - 1));
                    int right = Integer.parseInt(tokens.get(i + 1));
                    if (op.equals("/") && left % right != 0) {
                        right = randomDivisor(left);
                        tokens.set(i + 1, String.valueOf(right));
                    }
                    int result = op.equals("*") ? left * right : left / right;
                    solutionSteps.add(left + " " + op + " " + right + " = " + result);
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
                    int left  = Integer.parseInt(tokens.get(i - 1));
                    int right = Integer.parseInt(tokens.get(i + 1));
                    int result = op.equals("+") ? left + right : left - right;
                    solutionSteps.add(left + " " + op + " " + right + " = " + result);
                    tokens.set(i - 1, String.valueOf(result));
                    tokens.remove(i);
                    tokens.remove(i);
                    break;
                }
            }
        }

        // ── Step 4: build multiple-choice options (if requested) ──────────────
        int sum = Integer.parseInt(tokens.get(0));
        List<String> options = null;
        if (multipleChoice) {
            Set<Integer> unique = new LinkedHashSet<>();
            unique.add(sum);
            int spread = Math.max(1, (int) (Math.abs(sum) * DISTRACTOR_SPREAD));
            for (int attempts = 0; attempts < 30 && unique.size() < OPTIONS_COUNT; attempts++) {
                int distractor = sum + randomInt(-spread, spread + 1);
                if (distractor != sum) unique.add(distractor);
            }
            // Guaranteed fallback: sum+1, sum+2, … can never collide with each other or sum
            for (int i = 1; unique.size() < OPTIONS_COUNT; i++) unique.add(sum + i);

            options = new ArrayList<>();
            for (int v : unique) options.add(String.valueOf(v));
            Collections.shuffle(options, random);
        }

        return new Question(subSubject, expression, String.valueOf(sum),
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

    /** Returns a random proper divisor of {@code n} so division always yields a whole number. */
    private int randomDivisor(int n) {
        if (n == 0) return 1;
        List<Integer> divisors = new ArrayList<>();
        for (int i = 1; i <= Math.abs(n); i++) {
            if (n % i == 0) divisors.add(i);
        }
        return divisors.get(random.nextInt(divisors.size()));
    }
}
