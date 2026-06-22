package com.adaptive.server.service.errorpattern;

import org.springframework.stereotype.Component;

/**
 * Error patterns for the base arithmetic (Calculation) generator. Preserves the original
 * {@code LevelManagerService.analyzeErrorPattern} behaviour verbatim, and doubles as the default
 * fallback {@link ErrorPatternService} uses for any subject without a dedicated analyzer.
 */
@Component
public class ArithmeticErrorAnalyzer implements ErrorPatternAnalyzer {

    private static final String SUBJECT_NAME = "Calculation";

    @Override
    public boolean supports(String subjectName) {
        return SUBJECT_NAME.equalsIgnoreCase(subjectName);
    }

    @Override
    public String analyze(String expression, String correctAnswer, String userAnswer, String questionType) {
        try {
            int userVal = Integer.parseInt(userAnswer.trim());
            int correctVal = Integer.parseInt(correctAnswer.trim());
            if (questionType != null && questionType.contains("SUBTRACTION") && expression != null) {
                String[] parts = expression.split("-");
                if (parts.length == 2) {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    if (userVal == (a + b)) return "CONFUSED_SUB_WITH_ADD";
                }
            }
            if (Math.abs(userVal - correctVal) <= 2) return "MINOR_CALCULATION_ERROR";
        } catch (NumberFormatException e) {
            return "INVALID_FORMAT_ERROR";
        }
        return "GENERAL_ERROR_" + questionType;
    }
}
