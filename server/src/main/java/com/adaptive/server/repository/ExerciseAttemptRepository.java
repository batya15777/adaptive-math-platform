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
}
