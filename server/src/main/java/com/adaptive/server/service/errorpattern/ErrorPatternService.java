package com.adaptive.server.service.errorpattern;

import com.adaptive.server.entity.SubSubject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Single entry point the answer-submission flow uses to label a wrong answer for the ML pipeline.
 *
 * <p>Dispatches to the {@link ErrorPatternAnalyzer} that handles the question's subject, falling
 * back to the arithmetic analyzer for anything without a dedicated one. The chosen label is written
 * to {@code exercise_attempts.error_pattern} — the SAME unified record every generator feeds — so
 * adding a new generator never touches the tracking/ML logic, only (optionally) a new analyzer.
 */
@Service
public class ErrorPatternService {

    private final List<ErrorPatternAnalyzer> analyzers;
    private final ErrorPatternAnalyzer fallback;

    public ErrorPatternService(List<ErrorPatternAnalyzer> analyzers, ArithmeticErrorAnalyzer fallback) {
        this.analyzers = analyzers;
        this.fallback = fallback;
    }

    /**
     * Classifies a wrong answer for ML tracking. Never returns {@code null}.
     *
     * @param subSubject the question's sub-subject (used to resolve the owning subject → analyzer)
     */
    public String analyze(SubSubject subSubject, String expression, String correctAnswer,
                          String userAnswer, String questionType) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return "EMPTY_ANSWER";
        }
        String subjectName = (subSubject != null && subSubject.getSubject() != null)
                ? subSubject.getSubject().getName() : null;
        return pick(subjectName).analyze(expression, correctAnswer, userAnswer, questionType);
    }

    private ErrorPatternAnalyzer pick(String subjectName) {
        if (subjectName != null) {
            for (ErrorPatternAnalyzer analyzer : analyzers) {
                if (analyzer.supports(subjectName)) {
                    return analyzer;
                }
            }
        }
        return fallback;
    }
}
