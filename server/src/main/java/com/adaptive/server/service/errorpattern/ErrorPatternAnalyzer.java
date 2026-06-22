package com.adaptive.server.service.errorpattern;

/**
 * Strategy for turning a WRONG answer into a stable, ML-friendly error-pattern label.
 *
 * <p>One implementation per question family (arithmetic, polynomial, verbal/word problems);
 * {@link ErrorPatternService} dispatches by the question's subject. The label is written to
 * {@code exercise_attempts.error_pattern} — the single, unified record every generator already
 * feeds — which the clustering microservice TF-IDFs into a feature. Keeping this per-family means
 * the new generators emit meaningful signal instead of degenerate {@code INVALID_FORMAT_ERROR}s.
 */
public interface ErrorPatternAnalyzer {

    /** True if this analyzer handles questions under the given subject name (e.g. {@code "Polynomial"}). */
    boolean supports(String subjectName);

    /**
     * Classifies a wrong answer. The caller ({@link ErrorPatternService}) guarantees
     * {@code userAnswer} is non-blank. Implementations must never return {@code null} —
     * return a generic family label when nothing more specific fits.
     */
    String analyze(String expression, String correctAnswer, String userAnswer, String questionType);
}
