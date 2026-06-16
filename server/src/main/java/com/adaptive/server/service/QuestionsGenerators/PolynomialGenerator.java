//package com.adaptive.server.service.QuestionsGenerators;
//
//import com.adaptive.server.entity.Question;
//import com.adaptive.server.entity.SubSubject;
//import com.adaptive.server.entity.enums.PolynomialOperation;
//import com.adaptive.server.entity.enums.QuestionStatus;
//import com.adaptive.server.repository.QuestionTemplateRepository;
//import com.adaptive.server.repository.SubSubjectRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ThreadLocalRandom;
//
///**
// * Generates polynomial equations (linear and quadratic) with step-by-step solutions.
// * Answers are always whole numbers by construction.
// */
//@Service
//public class PolynomialGenerator extends QuestionGenerator<PolynomialOperation> {
//
//    private static final String SUBJECT_NAME = "Polynomial";
//
//    public PolynomialGenerator(QuestionTemplateRepository templateRepository,
//                               SubSubjectRepository subSubjectRepository) {
//        super(templateRepository, subSubjectRepository);
//    }
//
//    @Override
//    public Question createQuestion(PolynomialOperation subSubject, int subSubjectLevel, int difficultyLevel, String language, boolean multipleChoice) {
//        return null;
//    }
//
//    @Override
//    protected String getSubjectName() {
//        return SUBJECT_NAME;
//    }
//
//    @Override
//    public Question createQuestion(PolynomialOperation operation, int difficultyLevel,
//                                   String language, boolean multipleChoice) {
////        SubSubject subSubject = resolveSubSubject(operation.getSubSubjectName());
////
////        return switch (operation) {
////            case LINEAR    -> buildLinear(subSubject, difficultyLevel, language, multipleChoice);
////            case QUADRATIC -> buildQuadratic(subSubject, difficultyLevel, language, multipleChoice);
////        };
//        return null;
//    }
//
//    /**
//     * Bonus linear: ax + b = cx + d  (must collect like terms before solving).
//     * Bonus quadratic: 2x² + bx + c = 0  (leading coefficient is 2, not 1).
//     */
//    @Override
//    public Question createBonusQuestion(PolynomialOperation operation, String language, boolean multipleChoice) {
////        SubSubject subSubject = resolveSubSubject(operation.getSubSubjectName());
////        return switch (operation) {
////            case LINEAR    -> buildBonusLinear(subSubject, language, multipleChoice);
////            case QUADRATIC -> buildBonusQuadratic(subSubject, language, multipleChoice);
////        };
//        return null;
//    }
//
////    private Question buildBonusLinear(SubSubject subSubject, String language, boolean multipleChoice) {
////        // ax + b = cx + d  →  (a-c)x = d-b  →  x = (d-b)/(a-c)
////        int a, c;
////        do {
////            a = rand(5, 20);
////            c = rand(1, a - 1); // ensure a > c so coefficient is positive
////        } while (a == c);
////
////        int x = rand(2, 15);
////        int b = rand(1, 10);
////        int d = a * x + b - c * x; // derived so that the equation holds
////
////        String aStr = a + "x";
////        String cStr = c + "x";
////        String expression = aStr + " + " + b + " = " + cStr + " + " + d;
////        String answer     = "x = " + x;
////        int coeff = a - c;
////        int rhs   = d - b;
////        List<String> solution = List.of(
////                "Move x terms to left: " + a + "x - " + c + "x = " + d + " - " + b,
////                coeff + "x = " + rhs,
////                "x = " + rhs + " / " + coeff + " = " + x
////        );
////        List<String> options = multipleChoice ? nearbyIntegers(x) : new ArrayList<>();
////        return new Question(subSubject, expression, answer, solution, options,
////                language, 10, QuestionStatus.CURRENT);
////    }
////
////    private Question buildBonusQuadratic(SubSubject subSubject, String language, boolean multipleChoice) {
////        // 2x² + bx + c = 0  where roots r1, r2 satisfy 2(x-r1)(x-r2)=0
////        int r1 = rand(1, 10);
////        int r2 = rand(1, 10);
////        int b  = -2 * (r1 + r2);   // Vieta's for leading coeff 2
////        int cv = 2 * r1 * r2;
////
////        String bStr  = b >= 0 ? " + " + b : " - " + Math.abs(b);
////        String cvStr = cv >= 0 ? " + " + cv : " - " + Math.abs(cv);
////        String expression = "2x²" + bStr + "x" + cvStr + " = 0";
////        String answer     = "x₁ = " + r1 + ", x₂ = " + r2;
////
////        int discriminant = b * b - 4 * 2 * cv;
////        int sqrtD        = (int) Math.round(Math.sqrt(discriminant));
////        List<String> solution = List.of(
////                "Discriminant = b² - 4·2·c = " + b + "² - 8·" + cv + " = " + discriminant,
////                "√" + discriminant + " = " + sqrtD,
////                "x = (-" + b + " ± " + sqrtD + ") / (2·2) = (-" + b + " ± " + sqrtD + ") / 4",
////                "x₁ = " + r1 + ", x₂ = " + r2
////        );
////        List<String> options = multipleChoice ? quadraticOptions(r1, r2) : new ArrayList<>();
////        return new Question(subSubject, expression, answer, solution, options,
////                language, 10, QuestionStatus.CURRENT);
////    }
//
//    // ── Linear: ax + b = c ───────────────────────────────────────────────
//
////    private Question buildLinear(SubSubject subSubject, int difficultyLevel,
////                                 String language, boolean multipleChoice) {
////        int a = rand(1, Math.max(2, difficultyLevel * 3));
////        int x = rand(1, Math.max(2, difficultyLevel * 5)); // the actual answer
////        int b = rand(1, 10);
////        int c = a * x + b;
////
////        String expression = a + "x + " + b + " = " + c;
////        String answer     = String.valueOf(x);
////        List<String> solution = linearSolution(a, b, c, x);
////        List<String> options  = multipleChoice ? nearbyIntegers(x) : new ArrayList<>();
////
////        return new Question(subSubject, expression, answer, solution, options,
////                language, difficultyLevel, QuestionStatus.CURRENT);
////    }
////
////    private List<String> linearSolution(int a, int b, int c, int x) {
////        int rhs = c - b;
////        return List.of(
////                c + " - " + b + " = " + rhs + "  →  " + a + "x = " + rhs,
////                rhs + " / " + a + " = " + x,
////                "x = " + x
////        );
////    }
//
//    // ── Quadratic: x² + bx + c = 0 ──────────────────────────────────────
//
////    private Question buildQuadratic(SubSubject subSubject, int difficultyLevel,
////                                    String language, boolean multipleChoice) {
////        // Pick two integer roots then work backwards to coefficients (a=1)
////        int r1 = rand(1, Math.max(2, difficultyLevel * 3));
////        int r2 = rand(1, Math.max(2, difficultyLevel * 3));
////        int b  = -(r1 + r2); // Vieta's formula
////        int cv = r1 * r2;
////
////        String bStr  = b >= 0 ? " + " + b : " - " + Math.abs(b);
////        String cvStr = cv >= 0 ? " + " + cv : " - " + Math.abs(cv);
////        String expression = "x²" + bStr + "x" + cvStr + " = 0";
////        String answer     = "x₁ = " + r1 + ", x₂ = " + r2;
////        List<String> solution = quadraticSolution(b, cv, r1, r2);
////        List<String> options  = multipleChoice ? quadraticOptions(r1, r2) : new ArrayList<>();
////
////        return new Question(subSubject, expression, answer, solution, options,
////                language, difficultyLevel, QuestionStatus.CURRENT);
////    }
////
////    private List<String> quadraticSolution(int b, int c, int r1, int r2) {
////        int discriminant = b * b - 4 * c;
////        int sqrtD        = (int) Math.round(Math.sqrt(discriminant));
////        return List.of(
////                "Discriminant = b² - 4c = " + b + "² - 4·" + c + " = " + discriminant,
////                "√" + discriminant + " = " + sqrtD,
////                "x = (-" + b + " ± " + sqrtD + ") / 2",
////                "x₁ = " + r1 + ", x₂ = " + r2
////        );
////    }
////
////    private List<String> nearbyIntegers(int correct) {
////        List<String> opts = new ArrayList<>(List.of(
////                String.valueOf(correct),
////                String.valueOf(correct + 1),
////                String.valueOf(correct + 2),
////                String.valueOf(correct - 1 < 0 ? correct + 3 : correct - 1)
////        ));
////        java.util.Collections.shuffle(opts);
////        return opts;
////    }
////
////    private List<String> quadraticOptions(int r1, int r2) {
////        return List.of(
////                "x₁ = " + r1 + ", x₂ = " + r2,
////                "x₁ = " + (r1 + 1) + ", x₂ = " + r2,
////                "x₁ = " + r1 + ", x₂ = " + (r2 + 1),
////                "x₁ = " + (r1 - 1) + ", x₂ = " + (r2 - 1)
////        );
////    }
////
////    private int rand(int min, int max) {
////        return ThreadLocalRandom.current().nextInt(min, max + 1);
////    }
//}
