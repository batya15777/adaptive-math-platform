package com.adaptive.server.repository;

import com.adaptive.server.entity.GeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestion, Long> {

    List<GeneratedQuestion> findBySubSubjectIdAndLanguageAndDifficultyLevelAndMultipleChoice(
            Long subSubjectId, String language, Integer difficultyLevel, Boolean multipleChoice);
}
