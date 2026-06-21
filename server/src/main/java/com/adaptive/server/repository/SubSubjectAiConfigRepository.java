package com.adaptive.server.repository;

import com.adaptive.server.entity.SubSubjectAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubSubjectAiConfigRepository extends JpaRepository<SubSubjectAiConfig, Long> {
    Optional<SubSubjectAiConfig> findBySubSubjectId(Long subSubjectId);
}
