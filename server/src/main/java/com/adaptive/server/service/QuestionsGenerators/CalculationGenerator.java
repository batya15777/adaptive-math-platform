package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CalculationGenerator extends QuestionGenerator {

    private static final String SUBJECT_NAME = "Calculation";
    private static final int OPTIONS_COUNT = 4;
    private static final double DISTRACTOR_SPREAD = 0.15;
    private Random random = new Random();

    public CalculationGenerator(QuestionTemplateRepository templateRepository,
                                SubSubjectRepository subSubjectRepository) {
        super(templateRepository, subSubjectRepository);
    }

    @Override
    protected String getSubjectName() {
        return SUBJECT_NAME;
    }

    @Override
    public Question createQuestion(SubSubject subSubject,int subSubjectLevel, int difficultyLevel,
                                   String language, boolean multipleChoice) {
        List<QuestionTemplate> templates = templateRepository
                .findAllBySubSubjectAndDifficultyLevel(subSubject, difficultyLevel);
        // { "X + X", "X - X", "X * X", "X / X", ... }
        String templateExpression = "";
        templateExpression = templates.get(random.nextInt(templates.size())).getExpression();
//        templateExpression = "X + X * X";

        // Step 1: replace X's with random values
        List<String> tokens = new ArrayList<>(List.of(templateExpression.split(" ")));
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("X")) {
                String prevOp = (i > 0) ? tokens.get(i - 1) : "";
                String nextOp = (i < tokens.size() - 1) ? tokens.get(i + 1) : "";
                boolean nearMult = prevOp.equals("*") || nextOp.equals("*");
                int val = nearMult
                        ? randomInt(2, Math.max(3, difficultyLevel * 2))
                        : randomInt(subSubjectLevel + difficultyLevel, subSubjectLevel + difficultyLevel * 2);
                tokens.set(i, String.valueOf(val));
            }
        }
        String expression = String.join(" ", tokens); // what the student sees

        List<String> solutionSteps = new ArrayList<>();

        // Step 2: evaluate * and / first (left-to-right among them)
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

        // Step 3: evaluate + and -
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

        int sum = Integer.parseInt(tokens.get(0));
        List<String> options = null;
        if (multipleChoice) {
            Set<Integer> unique = new LinkedHashSet<>();
            unique.add(sum);
            int spread = Math.max(1, (int) (Math.abs(sum) * DISTRACTOR_SPREAD));
            // try random distractors within spread first
            for (int attempts = 0; attempts < 30 && unique.size() < OPTIONS_COUNT; attempts++) {
                int distractor = sum + randomInt(-spread, spread + 1);
                if (distractor != sum) unique.add(distractor);
            }
            // guaranteed fallback: sum+1, sum+2, ... can never collide with each other or sum
            for (int i = 1; unique.size() < OPTIONS_COUNT; i++) {
                unique.add(sum + i);
            }
            options = new ArrayList<>();
            for (int v : unique) options.add(String.valueOf(v));
            Collections.shuffle(options, random);
        }

        return new Question(subSubject, expression, String.valueOf(sum), solutionSteps, options,
                language,difficultyLevel,QuestionStatus.CURRENT
                );
    }

    private int randomInt(int min, int max){
        return random.nextInt(min,max);
    }

    private float randomFloat(int min, int max){
        return random.nextFloat(min,max);
    }

    // Returns a random divisor of n (always >= 1, so division is always whole)
    private int randomDivisor(int n) {
        if (n == 0) return 1;
        List<Integer> divisors = new ArrayList<>();
        for (int i = 1; i <= Math.abs(n); i++) {
            if (n % i == 0) divisors.add(i);
        }
        return divisors.get(random.nextInt(divisors.size()));
    }

    /**
     * Bonus: forces a multi-operator expression with parentheses regardless of DB templates.
     * Difficulty is fixed at 10 so placeholder values are large.
     */
//    @Override
//    public Question createBonusQuestion(CalculationOperation operation, String language, boolean multipleChoice) {
//
//        return null;
//    }
}
