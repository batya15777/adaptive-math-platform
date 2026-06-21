package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Generates simple <strong>linear "solve for x"</strong> equations, e.g.
 * {@code 3x + 7 = 0}, {@code 5x = 20}, {@code 4x - 8 = 2x + 6}, {@code x / 3 = 14 - 2x}.
 *
 * <p>Works like {@link CalculationGenerator}: coefficients are random numbers whose size
 * scales with difficulty (small and &lt; 100 for difficulty 1–4, larger above). The unknown
 * is the literal {@code x}.
 *
 * <p>To guarantee a clean answer for the exact-string answer check, equations are built by
 * <em>backward construction</em>: a nice integer solution {@code x} is chosen first, then
 * coefficients are generated and the remaining constant is derived so the equation holds.
 * The solution is therefore always an integer.
 */
@Service
public class PolynomialGenerator extends QuestionGenerator {

    private static final String SUBJECT_NAME = "Polynomial";
    private static final int    OPTIONS_COUNT = 4;

    private static final int MAX_DIFFICULTY_LEVEL = 10;
    private static final int LOW_REGIME_MAX = 4;          // diff 1–4 → numbers & solution < 100
    private static final int LOW_REGIME_CEILING = 100;
    private static final int MAX_FILL_ATTEMPTS = 40;
    private static final int FORM_COUNT = 5;          // linear forms
    private static final int QUAD_FORM_COUNT = 3;     // quadratic display variants

    /** Sub-subjects whose name contains this word use the quadratic (two-root) generator. */
    private static final String QUADRATIC_MARKER = "quadratic";

    private final Random random = new Random();

    public PolynomialGenerator(QuestionTemplateRepository templateRepository,
                               SubSubjectRepository subSubjectRepository) {
        super(templateRepository, subSubjectRepository);
    }

    @Override
    protected String getSubjectName() {
        return SUBJECT_NAME;
    }

    /** Cluster-aware entry point — applies the cohort difficulty bias, then generates. */
    public Question createQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                   String language, boolean multipleChoice, ClusterContext cluster) {
        return createQuestion(subSubject, subSubjectLevel, applyClusterBias(difficultyLevel, cluster),
                language, multipleChoice);
    }

    private int applyClusterBias(int difficultyLevel, ClusterContext cluster) {
        if (cluster == null || !cluster.isAssigned() || cluster.getDifficultyBias() == 0) {
            return difficultyLevel;
        }
        int adjusted = difficultyLevel + cluster.getDifficultyBias();
        return Math.max(1, Math.min(adjusted, MAX_DIFFICULTY_LEVEL));
    }

    @Override
    public Question createQuestion(SubSubject subSubject, int subSubjectLevel, int difficultyLevel,
                                   String language, boolean multipleChoice) {

        int difficulty = Math.max(1, Math.min(difficultyLevel, MAX_DIFFICULTY_LEVEL));

        // Quadratic sub-subject: two-root equations, always multiple choice.
        if (isQuadratic(subSubject)) {
            return createQuadratic(subSubject, difficulty, subSubjectLevel, language);
        }

        Eq eq = null;
        for (int attempt = 0; attempt < MAX_FILL_ATTEMPTS; attempt++) {
            Eq candidate = buildForm(random.nextInt(FORM_COUNT), difficulty, subSubjectLevel);
            if (difficulty > LOW_REGIME_MAX || withinLowRegime(candidate.expression(), candidate.answer())) {
                eq = candidate;
                break;
            }
        }
        if (eq == null) { // constraints never satisfied — accept a fresh attempt
            eq = buildForm(random.nextInt(FORM_COUNT), difficulty, subSubjectLevel);
        }

        List<String> options = multipleChoice ? buildOptions(eq.answer()) : null;
        return new Question(subSubject, eq.expression(), String.valueOf(eq.answer()),
                eq.steps(), options, language, difficulty, QuestionStatus.CURRENT);
    }

    // ── Equation forms (backward construction → integer solution) ───────────────

    private Eq buildForm(int form, int difficulty, int ssl) {
        switch (form) {
            case 0:  return formAxPlusBEqualsZero(difficulty, ssl);
            case 1:  return formAxEqualsB(difficulty, ssl);
            case 2:  return formAxPlusBEqualsC(difficulty, ssl);
            case 3:  return formAxPlusBEqualsCxPlusD(difficulty, ssl);
            default: return formXOverAEqualsBMinusCx(difficulty, ssl);
        }
    }

    /** a*x + b = 0  →  b = -a*x. */
    private Eq formAxPlusBEqualsZero(int difficulty, int ssl) {
        int a = pickCoef(difficulty, ssl);
        int x = pickX(difficulty);
        int b = -a * x;
        String expression = term(a) + signTerm(b) + " = 0";
        List<String> steps = new ArrayList<>();
        steps.add(term(a) + " = " + (-b));
        if (Math.abs(a) != 1) steps.add(solveStep(-b, a, x));
        return new Eq(expression, x, steps);
    }

    /** a*x = b  →  b = a*x. */
    private Eq formAxEqualsB(int difficulty, int ssl) {
        int a = pickCoef(difficulty, ssl);
        int x = pickX(difficulty);
        int b = a * x;
        String expression = term(a) + " = " + b;
        List<String> steps = new ArrayList<>();
        steps.add(solveStep(b, a, x));
        return new Eq(expression, x, steps);
    }

    /** a*x + b = c  →  c = a*x + b. */
    private Eq formAxPlusBEqualsC(int difficulty, int ssl) {
        int a = pickCoef(difficulty, ssl);
        int x = pickX(difficulty);
        int b = pickConst(difficulty, ssl);
        int c = a * x + b;
        String expression = term(a) + signTerm(b) + " = " + c;
        List<String> steps = new ArrayList<>();
        steps.add(term(a) + " = " + (c - b));           // move b across
        if (Math.abs(a) != 1) steps.add(solveStep(c - b, a, x));
        return new Eq(expression, x, steps);
    }

    /** a*x + b = c*x + d  →  d = (a-c)*x + b, with a != c. */
    private Eq formAxPlusBEqualsCxPlusD(int difficulty, int ssl) {
        int a = pickCoef(difficulty, ssl);
        int c = pickCoef(difficulty, ssl);
        while (c == a) c = pickCoef(difficulty, ssl);
        int x = pickX(difficulty);
        int b = pickConst(difficulty, ssl);
        int d = (a - c) * x + b;
        String expression = term(a) + signTerm(b) + " = " + term(c) + signTerm(d);
        List<String> steps = new ArrayList<>();
        steps.add(term(a - c) + " = " + (d - b));        // collect x left, constants right
        if (Math.abs(a - c) != 1) steps.add(solveStep(d - b, a - c, x));
        return new Eq(expression, x, steps);
    }

    /** x / a = b - c*x. Keep integers: x = a*k, b = k + c*x. */
    private Eq formXOverAEqualsBMinusCx(int difficulty, int ssl) {
        boolean low = difficulty <= LOW_REGIME_MAX;
        int a = randomInt(2, (low ? 4 : 6 + difficulty / 2) + 1);
        int c = randomInt(2, (low ? 4 : 6 + difficulty / 2) + 1);
        int k = randomInt(1, (low ? 3 : 2 + difficulty / 2) + 1);
        int x = a * k;
        int b = k + c * x;
        String expression = "x / " + a + " = " + b + " - " + term(c);
        List<String> steps = new ArrayList<>();
        steps.add("x = " + (a * b) + " - " + (a * c) + "x");   // multiply both sides by a
        steps.add(term(1 + a * c) + " = " + (a * b));          // collect x
        steps.add(solveStep(a * b, 1 + a * c, x));
        return new Eq(expression, x, steps);
    }

    // ── Difficulty-scaled coefficient picking ───────────────────────────────────

    private int coefMax(int difficulty, int ssl) {
        return difficulty <= LOW_REGIME_MAX ? 9 : (5 + 2 * difficulty + 2 * ssl);
    }

    /** Positive coefficient (≥ 2) for the x terms. */
    private int pickCoef(int difficulty, int ssl) {
        return randomInt(2, coefMax(difficulty, ssl) + 1);
    }

    /** Signed, non-zero integer solution. */
    private int pickX(int difficulty) {
        int max = difficulty <= LOW_REGIME_MAX ? 9 : (4 + difficulty);
        int magnitude = randomInt(1, max + 1);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    /** Signed constant term. */
    private int pickConst(int difficulty, int ssl) {
        int max = coefMax(difficulty, ssl);
        int magnitude = randomInt(1, max + 1);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    /** True when every displayed number and the solution are below {@value #LOW_REGIME_CEILING}. */
    private boolean withinLowRegime(String expression, int answer) {
        return Math.abs(answer) < LOW_REGIME_CEILING && allNumbersBelowCeiling(expression);
    }

    /** True when every numeric literal in the expression is below {@value #LOW_REGIME_CEILING}. */
    private boolean allNumbersBelowCeiling(String expression) {
        for (String tok : expression.replaceAll("[^0-9]", " ").trim().split("\\s+")) {
            if (!tok.isEmpty() && Long.parseLong(tok) >= LOW_REGIME_CEILING) return false;
        }
        return true;
    }

    // ── Quadratic (two-root) equations ──────────────────────────────────────────

    private boolean isQuadratic(SubSubject subSubject) {
        return subSubject.getName() != null
                && subSubject.getName().toLowerCase().contains(QUADRATIC_MARKER);
    }

    /**
     * Builds a quadratic with two distinct integer roots via backward construction:
     * pick roots r1, r2 → monic quadratic x² - (r1+r2)x + r1*r2 = 0, rendered in one of a
     * few equivalent display forms. Always multiple choice; the answer is the root pair
     * string "X1=lo , X2=hi".
     */
    private Question createQuadratic(SubSubject subSubject, int difficulty, int ssl, String language) {
        int lo = 0, hi = 0;
        String expression = null;
        List<String> steps = null;

        for (int attempt = 0; attempt < MAX_FILL_ATTEMPTS; attempt++) {
            int r1 = pickRoot(difficulty);
            int r2 = pickRoot(difficulty);
            while (r2 == r1 || r1 + r2 == 0) r2 = pickRoot(difficulty); // distinct, and b != 0
            int a = Math.min(r1, r2), b = Math.max(r1, r2);
            QExpr q = renderQuadratic(random.nextInt(QUAD_FORM_COUNT), a, b);
            if (difficulty > LOW_REGIME_MAX || allNumbersBelowCeiling(q.expression())) {
                lo = a; hi = b; expression = q.expression(); steps = q.steps();
                break;
            }
        }
        if (expression == null) { // fallback — accept a fresh pair
            int r1 = pickRoot(difficulty), r2 = pickRoot(difficulty);
            while (r2 == r1 || r1 + r2 == 0) r2 = pickRoot(difficulty);
            lo = Math.min(r1, r2); hi = Math.max(r1, r2);
            QExpr q = renderQuadratic(0, lo, hi);
            expression = q.expression(); steps = q.steps();
        }

        String answer = rootsString(lo, hi);
        List<String> options = buildRootOptions(lo, hi); // quadratic is always multiple choice
        return new Question(subSubject, expression, answer, steps, options, language, difficulty,
                QuestionStatus.CURRENT);
    }

    /** A non-zero integer root whose magnitude scales with difficulty. */
    private int pickRoot(int difficulty) {
        int max = difficulty <= LOW_REGIME_MAX ? 9 : (4 + difficulty);
        int magnitude = randomInt(2, max + 1);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    /** Renders the monic quadratic with roots (lo, hi) in one of three equivalent forms. */
    private QExpr renderQuadratic(int form, int lo, int hi) {
        int b = -(lo + hi);   // coefficient of x
        int c = lo * hi;      // constant term
        String expression;
        switch (form) {
            case 1:  expression = "x²" + linTerm(b) + " = " + (-c); break;     // x² + bx = -c
            case 2:  expression = "x² = " + buildRhs(-b, -c); break;           // x² = -bx - c
            default: expression = "x²" + linTerm(b) + constTerm(c) + " = 0";   // x² + bx + c = 0
        }
        List<String> steps = new ArrayList<>();
        steps.add("(x " + factorSign(lo) + ")(x " + factorSign(hi) + ") = 0");
        steps.add("x = " + lo + "  or  x = " + hi);
        return new QExpr(expression, steps);
    }

    /** Coefficient·x middle term with a leading sign, e.g. " - 15x", " + x". */
    private String linTerm(int b) {
        if (b == 0) return "";
        String sign = b > 0 ? " + " : " - ";
        int m = Math.abs(b);
        return sign + (m == 1 ? "x" : m + "x");
    }

    /** Constant term with a leading sign, e.g. " + 50", " - 6". */
    private String constTerm(int c) {
        if (c == 0) return "";
        return (c > 0 ? " + " : " - ") + Math.abs(c);
    }

    /** Right-hand side "coefX·x + con" (coefX is non-zero here). */
    private String buildRhs(int coefX, int con) {
        String s = (coefX == 1) ? "x" : (coefX == -1) ? "-x" : coefX + "x";
        if (con != 0) {
            s += (con > 0 ? " + " + con : " - " + (-con));
        }
        return s;
    }

    /** Factor sign for "(x - r)" / "(x + |r|)". */
    private String factorSign(int r) {
        return r >= 0 ? "- " + r : "+ " + (-r);
    }

    /** The canonical root-pair answer string, e.g. "X1=5 , X2=10". */
    private String rootsString(int lo, int hi) {
        return "X1=" + lo + " , X2=" + hi;
    }

    /** Four distinct root-pair options (correct + plausible perturbations), shuffled. */
    private List<String> buildRootOptions(int lo, int hi) {
        Set<String> options = new LinkedHashSet<>();
        options.add(rootsString(lo, hi));
        for (int guard = 0; guard < 60 && options.size() < OPTIONS_COUNT; guard++) {
            int d1 = lo + randomInt(-3, 4);
            int d2 = hi + randomInt(-3, 4);
            if (d1 == d2) continue;
            options.add(rootsString(Math.min(d1, d2), Math.max(d1, d2)));
        }
        for (int k = 1; options.size() < OPTIONS_COUNT; k++) {
            options.add(rootsString(lo, hi + k));
        }
        List<String> list = new ArrayList<>(options);
        Collections.shuffle(list, random);
        return list;
    }

    // ── Rendering helpers ───────────────────────────────────────────────────────

    /** A coefficient·x term: 1→"x", -1→"-x", else "{coef}x". */
    private String term(int coef) {
        if (coef == 1) return "x";
        if (coef == -1) return "-x";
        return coef + "x";
    }

    /** A signed constant for appending, e.g. " + 7" or " - 7". */
    private String signTerm(int c) {
        return c >= 0 ? " + " + c : " - " + (-c);
    }

    /** Final solve step "x = num / den = answer", collapsing a unit denominator. */
    private String solveStep(int num, int den, int answer) {
        if (den == 1)  return "x = " + num;
        if (den == -1) return "x = " + (-num);
        return "x = " + num + " / " + den + " = " + answer;
    }

    // ── Multiple choice ─────────────────────────────────────────────────────────

    private List<String> buildOptions(int answer) {
        Set<String> unique = new LinkedHashSet<>();
        unique.add(String.valueOf(answer));
        for (int attempts = 0; attempts < 40 && unique.size() < OPTIONS_COUNT; attempts++) {
            int k = randomInt(1, 5); // 1..4 away
            int distractor = answer + (random.nextBoolean() ? k : -k);
            unique.add(String.valueOf(distractor));
        }
        for (int k = 1; unique.size() < OPTIONS_COUNT; k++) {
            unique.add(String.valueOf(answer + k));
        }
        List<String> options = new ArrayList<>(unique);
        Collections.shuffle(options, random);
        return options;
    }

    private int randomInt(int min, int max) {
        return random.nextInt(min, max);
    }

    /** A built linear equation: the displayed string, its integer solution, and solution steps. */
    private record Eq(String expression, int answer, List<String> steps) {}

    /** A built quadratic display: the equation string and its solution steps. */
    private record QExpr(String expression, List<String> steps) {}
}
