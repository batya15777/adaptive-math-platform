//package com.adaptive.server.service.QuestionsGenerators;
//
//import com.adaptive.server.entity.Question;
//import com.adaptive.server.entity.SubSubject;
//import com.adaptive.server.entity.enums.GeometryOperation;
//import com.adaptive.server.entity.enums.QuestionStatus;
//import com.adaptive.server.repository.QuestionTemplateRepository;
//import com.adaptive.server.repository.SubSubjectRepository;
//import org.springframework.stereotype.Service;
//
//import java.text.DecimalFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ThreadLocalRandom;
//
///**
// * Generates geometry questions (area, perimeter) without DB templates.
// * Each GeometryOperation knows its own formula, question text, and solution steps.
// */
//@Service
//public class GeometryGenerator extends QuestionGenerator<GeometryOperation> {
//
//    private static final String SUBJECT_NAME = "Geometry";
//    private static final int OPTIONS_COUNT = 4;
//    private static final double DISTRACTOR_SPREAD = 0.15;
//
//    public GeometryGenerator(QuestionTemplateRepository templateRepository,
//                             SubSubjectRepository subSubjectRepository) {
//        super(templateRepository, subSubjectRepository);
//    }
//
//    @Override
//    public Question createQuestion(GeometryOperation subSubject, int subSubjectLevel, int difficultyLevel, String language, boolean multipleChoice) {
//        return null;
//    }
//
//    @Override
//    protected String getSubjectName() {
//        return SUBJECT_NAME;
//    }
//
//    @Override
//    public Question createQuestion(GeometryOperation operation, int difficultyLevel,
//                                   String language, boolean multipleChoice) {
////        SubSubject subSubject = resolveSubSubject(operation.getSubSubjectName());
////
////        // Scale random dimension values with difficulty (range: difficulty to 8*difficulty)
////        int w = randomDimension(difficultyLevel);
////        int h = randomDimension(difficultyLevel);
////        int b = randomDimension(difficultyLevel);
////        int r = randomDimension(difficultyLevel);
////
////        String questionText = operation.buildQuestion(w, h, b, r);
////        double result       = operation.compute(w, h, b, r);
////        List<String> solution = operation.buildSolution(w, h, b, r);
////        List<String> options  = multipleChoice ? buildOptions(result) : new ArrayList<>();
////
////        return new Question(subSubject, questionText, format.format(result),
////                solution, options, language, difficultyLevel, QuestionStatus.CURRENT);
//        return null;
//    }
//
//    /**
//     * Bonus: compound shape — find the total area of a rectangle plus a triangle
//     * that share the same height. Ignores the specific operation; always produces
//     * one combined multi-step problem.
//     */
//    @Override
//    public Question createBonusQuestion(GeometryOperation operation, String language, boolean multipleChoice) {
////        SubSubject subSubject = resolveSubSubject(operation.getSubSubjectName());
////
////        int w = randomDimension(10);
////        int h = randomDimension(10);
////        int b = randomDimension(10);
////
////        double rectArea     = (double) w * h;
////        double triangleArea = (b * h) / 2.0;
////        double total        = rectArea + triangleArea;
////
////        String questionText = "A compound shape consists of a rectangle (width " + w +
////                ", height " + h + ") and a triangle (base " + b + ", height " + h +
////                "). Find the total area.";
////
////        List<String> solution = List.of(
////                "Rectangle area: " + w + " * " + h + " = " + fmt(rectArea),
////                "Triangle area: (" + b + " * " + h + ") / 2 = " + fmt(triangleArea),
////                "Total area: " + fmt(rectArea) + " + " + fmt(triangleArea) + " = " + fmt(total)
////        );
////        List<String> options = multipleChoice ? buildOptions(total) : new ArrayList<>();
////
////        return new Question(subSubject, questionText, fmt(total),
////                solution, options, language, 10, QuestionStatus.CURRENT);
//        return null;
//    }
//
////    private int randomDimension(int difficultyLevel) {
////        int min = Math.max(1, difficultyLevel);
////        int max = Math.max(min + 1, 8 * difficultyLevel);
////        return ThreadLocalRandom.current().nextInt(min, max);
////    }
//
////    private List<String> buildOptions(double correctResult) {
////        Set<String> unique = new LinkedHashSet<>();
////        unique.add(format.format(correctResult));
////
////        double spread = Math.max(1.0, Math.abs(correctResult) * DISTRACTOR_SPREAD);
////
////        while (unique.size() < OPTIONS_COUNT) {
////            double offset = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * spread;
////            if (Math.abs(offset) < 0.01) continue;
////            unique.add(format.format(correctResult + offset));
////        }
////
////        List<String> options = new ArrayList<>(unique);
////        Collections.shuffle(options);
////        return options;
////    }
//}
