package com.adaptive.server.repository;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.enums.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySubSubjectId(Long subSubjectId);

    List<Question> findBySubSubjectIdAndStatus(Long subSubjectId, QuestionStatus status);

    List<Question> findBySubSubjectIdAndDifficultyLevel(Long subSubjectId, Integer difficultyLevel);

    List<Question> findByLanguage(String language);
}
