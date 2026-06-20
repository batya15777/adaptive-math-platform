package com.adaptive.server.service.QuestionsGenerators;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of a student's ML cohort, consumed by BOTH question
 * generators (the code-based {@link CalculationGenerator} and the AI
 * {@code AiQuestionService}) so each can adapt the next question.
 *
 * It is intentionally generator-agnostic: it carries the cohort id/label, the
 * cohort's dominant error patterns, and a derived difficulty bias. Generators
 * never touch the database for clustering — they just read this object.
 */
public class ClusterContext {

    private final Integer clusterId;
    private final String label;
    private final List<String> topErrorPatterns;
    private final int difficultyBias; // -1 easier · 0 neutral · +1 harder

    private ClusterContext(Integer clusterId, String label,
                           List<String> topErrorPatterns, int difficultyBias) {
        this.clusterId = clusterId;
        this.label = label;
        this.topErrorPatterns = topErrorPatterns == null
                ? Collections.emptyList() : List.copyOf(topErrorPatterns);
        this.difficultyBias = difficultyBias;
    }

    /** A no-op context for students with no cluster yet (generators behave as before). */
    public static ClusterContext neutral() {
        return new ClusterContext(null, null, Collections.emptyList(), 0);
    }

    public static ClusterContext of(Integer clusterId, String label,
                                    List<String> topErrorPatterns, int difficultyBias) {
        return new ClusterContext(clusterId, label, topErrorPatterns, difficultyBias);
    }

    public boolean isAssigned() {
        return clusterId != null;
    }

    public Integer getClusterId() { return clusterId; }

    public String getLabel() { return label; }

    public List<String> getTopErrorPatterns() { return topErrorPatterns; }

    /** The single most prominent weakness of the cohort, or null. */
    public String getFocusErrorPattern() {
        return topErrorPatterns.isEmpty() ? null : topErrorPatterns.get(0);
    }

    public int getDifficultyBias() { return difficultyBias; }
}
