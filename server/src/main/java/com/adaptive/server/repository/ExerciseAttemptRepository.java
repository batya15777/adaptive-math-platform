package com.adaptive.server.repository;

import com.adaptive.server.entity.ExerciseAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
