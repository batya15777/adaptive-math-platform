package com.adaptive.server.service.errorpattern;

import org.springframework.stereotype.Component;

/**
 * Error patterns for AI-generated verbal / word-problem questions (subjects {@code "Verbal Problems"}
 * and {@code "AI"}). Answers are numeric, so we classify by how far off the student landed: a small
 * relative miss reads as an arithmetic slip; a large one as a misunderstood problem setup. This gives
 * the clustering a usable signal instead of the arithmetic analyzer's int-only verdicts.
 */
@Component
public class VerbalErrorAnalyzer implements ErrorPatternAnalyzer {

    @Override
    public boolean supports(String subjectName) {
        return "Verbal Problems".equalsIgnoreCase(subjectName) || "AI".equalsIgnoreCase(subjectName);
    }

    @Override
    public String analyze(String expression, String correctAnswer, String userAnswer, String questionType) {
        Double user = parseNumber(userAnswer);
        Double correct = parseNumber(correctAnswer);
        if (user == null || correct == null) return "VERBAL_NON_NUMERIC_ANSWER";
        if (correct != 0 && user.equals(-correct)) return "VERBAL_SIGN_ERROR";
        // Within 10% (or ±1 for small numbers) → arithmetic slip; otherwise a wrong approach.
        double tolerance = Math.max(1.0, Math.abs(correct) * 0.1);
        if (Math.abs(user - correct) <= tolerance) return "VERBAL_CALCULATION_SLIP";
        return "VERBAL_WRONG_SETUP";
    }

    private Double parseNumber(String s) {
        if (s == null) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
