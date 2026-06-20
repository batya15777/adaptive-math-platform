package com.adaptive.server.repository;

import java.time.LocalDateTime;
//זה נקרא JPA PROJECTION לוקחים רק מה שצריך
/**
 * Read-only projection over {@link com.adaptive.server.entity.ExerciseAttempt},
 * pulling only the ML feature columns in a single query (avoids loading entities
 * and the lazy User association just to read its id).
 */
public interface AttemptFeatureProjection {
    Long getUserId();
    String getErrorPattern();
    String getQuestionType();
    Integer getDifficultyLevel();
    Boolean getIsCorrect();
    String getUserAnswer();
    LocalDateTime getAnsweredAt();
}
