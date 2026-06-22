package com.adaptive.server.repository;

import com.adaptive.server.entity.ExerciseAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {
    //שומרים היסטוריה של כל תרגיל שתלמיד פתר, מביאים ניסיונות עבר של תלמיד לפי נושא
    List<ExerciseAttempt> findByUserIdAndSubSubjectId(
            Long userId, Long subSubjectId, Pageable pageable);

    List<ExerciseAttempt> findByUserIdAndSubSubjectIdAndQuestionType(
            Long userId, Long subSubjectId, String questionType, Pageable pageable);

    long countByUserIdAndSubSubjectId(Long userId, Long subSubjectId);

    // Correct answers only — used by the Math Training page for "questions solved".
    long countByUserIdAndSubSubjectIdAndIsCorrectTrue(Long userId, Long subSubjectId);

    // All attempts as a lightweight ML-feature projection, ordered by user then time
    // so the clustering service can group them per student and compute timing features.
    @Query("SELECT a.user.id AS userId, a.errorPattern AS errorPattern, a.questionType AS questionType, " +
           "a.difficultyLevel AS difficultyLevel, a.isCorrect AS isCorrect, a.userAnswer AS userAnswer, " +
           "a.answeredAt AS answeredAt " +
           "FROM ExerciseAttempt a ORDER BY a.user.id ASC, a.answeredAt ASC")
    List<AttemptFeatureProjection> findAllAttemptFeatures();

    // Same ML-feature projection, but only for attempts by users whose CURRENT role
    // is :role — so ex-students who became admins are excluded from clustering input.
    @Query("SELECT a.user.id AS userId, a.errorPattern AS errorPattern, a.questionType AS questionType, " +
           "a.difficultyLevel AS difficultyLevel, a.isCorrect AS isCorrect, a.userAnswer AS userAnswer, " +
           "a.answeredAt AS answeredAt " +
           "FROM ExerciseAttempt a WHERE a.user.role = :role ORDER BY a.user.id ASC, a.answeredAt ASC")
    List<AttemptFeatureProjection> findAllAttemptFeaturesForRole(@Param("role") String role);

    // Dashboard: per-sub-subject totals + correct counts for one student, in one query.
    @Query("SELECT a.subSubject.id AS subSubjectId, a.subSubject.name AS subSubjectName, " +
           "COUNT(a) AS total, SUM(CASE WHEN a.isCorrect = true THEN 1L ELSE 0L END) AS correct " +
           "FROM ExerciseAttempt a WHERE a.user.id = :userId " +
           "GROUP BY a.subSubject.id, a.subSubject.name")
    List<TopicStatsProjection> findTopicStatsByUser(@Param("userId") Long userId);

    // Dashboard fallback signal: the student's own most frequent error patterns.
    @Query("SELECT a.errorPattern AS errorPattern, COUNT(a) AS occurrences FROM ExerciseAttempt a " +
           "WHERE a.user.id = :userId AND a.isCorrect = false AND a.errorPattern IS NOT NULL " +
           "GROUP BY a.errorPattern ORDER BY COUNT(a) DESC")
    List<ErrorPatternCountProjection> findErrorPatternCountsByUser(@Param("userId") Long userId);

    // ── Admin Analytics (read-only) — scoped to attempts by users whose CURRENT
    // role is the given one (so ex-students who became admins are excluded). ──────
    @Query("SELECT COUNT(a) FROM ExerciseAttempt a WHERE a.user.role = :role")
    long countAttemptsByUserRole(@Param("role") String role);

    @Query("SELECT COUNT(a) FROM ExerciseAttempt a WHERE a.user.role = :role AND a.isCorrect = true")
    long countCorrectAttemptsByUserRole(@Param("role") String role);

    // Distinct active learners (current role = :role) with an attempt since :since.
    @Query("SELECT COUNT(DISTINCT a.user.id) FROM ExerciseAttempt a " +
           "WHERE a.user.role = :role AND a.answeredAt >= :since")
    long countDistinctActiveStudentsSince(@Param("role") String role, @Param("since") LocalDateTime since);

    // Per-sub-subject totals + correct counts across users with role = :role (one query).
    // The min-attempts threshold + ordering are applied in the service.
    @Query("SELECT a.subSubject.id AS subSubjectId, a.subSubject.name AS subSubjectName, " +
           "COUNT(a) AS total, SUM(CASE WHEN a.isCorrect = true THEN 1L ELSE 0L END) AS correct " +
           "FROM ExerciseAttempt a WHERE a.user.role = :role " +
           "GROUP BY a.subSubject.id, a.subSubject.name")
    List<TopicStatsProjection> findSubSubjectStatsForRole(@Param("role") String role);

    // Most frequent error patterns across users with role = :role (wrong answers only).
    @Query("SELECT a.errorPattern AS errorPattern, COUNT(a) AS occurrences FROM ExerciseAttempt a " +
           "WHERE a.user.role = :role AND a.isCorrect = false AND a.errorPattern IS NOT NULL " +
           "GROUP BY a.errorPattern ORDER BY COUNT(a) DESC")
    List<ErrorPatternCountProjection> findErrorPatternCountsForRole(@Param("role") String role);
}
