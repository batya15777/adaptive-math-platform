package com.adaptive.server.service.errorpattern;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Error patterns for the {@code PolynomialGenerator}. Quadratics answer with a root-pair string
 * like {@code "X1=5 , X2=10"}; linear forms answer with a single integer. The arithmetic analyzer
 * would label every non-numeric root string {@code INVALID_FORMAT_ERROR}; this one extracts the
 * actual root values and reports usable mistakes (sign flips, one root right, off-by-one).
 */
@Component
public class PolynomialErrorAnalyzer implements ErrorPatternAnalyzer {

    private static final String SUBJECT_NAME = "Polynomial";
    private static final Pattern VALUE_AFTER_EQUALS = Pattern.compile("=\\s*(-?\\d+)");
    private static final Pattern ANY_INT = Pattern.compile("-?\\d+");

    @Override
    public boolean supports(String subjectName) {
        return SUBJECT_NAME.equalsIgnoreCase(subjectName);
    }

    @Override
    public String analyze(String expression, String correctAnswer, String userAnswer, String questionType) {
        int[] correct = extractValues(correctAnswer);
        int[] user = extractValues(userAnswer);

        // Quadratic root-pair answers → two values on each side.
        if (correct.length == 2 && user.length == 2) {
            if (contains(correct, user[0]) || contains(correct, user[1])) return "POLY_ONE_ROOT_CORRECT";
            if (isNegationOf(correct, user)) return "POLY_SIGN_ERROR";
            return "POLY_WRONG_ROOTS";
        }

        // Linear / single-value answers → one value on each side.
        if (correct.length >= 1 && user.length >= 1) {
            int c = correct[0], u = user[0];
            if (c != 0 && u == -c) return "POLY_SIGN_ERROR";
            if (Math.abs(u - c) <= 1) return "POLY_OFF_BY_ONE";
            return "POLY_WRONG_VALUE";
        }
        return "POLY_UNPARSED_ANSWER";
    }

    /**
     * Pulls the integer values out of an answer. For root-pair strings ("X1=5 , X2=10") it takes
     * the value after each '=' (so the X1/X2 labels' digits are ignored); otherwise it takes the
     * first integer it finds (a linear answer like "8" or "x = 8").
     */
    private int[] extractValues(String s) {
        if (s == null) return new int[0];
        List<Integer> out = new ArrayList<>();
        if (s.contains("=")) {
            Matcher m = VALUE_AFTER_EQUALS.matcher(s);
            while (m.find()) out.add(Integer.parseInt(m.group(1)));
        } else {
            Matcher m = ANY_INT.matcher(s);
            if (m.find()) out.add(Integer.parseInt(m.group()));
        }
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    private boolean contains(int[] arr, int v) {
        for (int x : arr) if (x == v) return true;
        return false;
    }

    /** True when the user's roots are the sign-flipped correct roots (a common factoring slip). */
    private boolean isNegationOf(int[] correct, int[] user) {
        return contains(correct, -user[0]) && contains(correct, -user[1]);
    }
}
