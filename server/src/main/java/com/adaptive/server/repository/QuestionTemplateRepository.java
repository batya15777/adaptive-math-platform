package com.adaptive.server.repository;

import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    public List<QuestionTemplate> findAllBySubSubject_Name(String name);

    public List<QuestionTemplate> findAllBySubSubjectAndDifficultyLevel(SubSubject subSubject, Integer difficultyLevel);

    public List<QuestionTemplate> findAllBySubSubjectAndDifficultyLevelAndSubSubjectLevel(SubSubject subSubject, Integer difficultyLevel, Integer subSubjectLevel);

    @Query("SELECT COALESCE(MAX(t.difficultyLevel), 1) FROM QuestionTemplate t WHERE t.subSubject = :subSubject")
    Integer findMaxDifficultyLevelBySubSubject(@Param("subSubject") SubSubject subSubject);
}
