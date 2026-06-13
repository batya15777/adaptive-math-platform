package com.adaptive.server.repository;

import com.adaptive.server.entity.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    public List<QuestionTemplate> findAllBySubSubject_Name(String name);
}
