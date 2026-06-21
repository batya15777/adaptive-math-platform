package com.adaptive.server.repository;

import com.adaptive.server.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    List<Subject> findByTopicId(Long topicId);

    long countByTopicId(Long topicId);

    // Admin list — all subjects of a topic (active + draft), ordered by name.
    List<Subject> findByTopicIdOrderByNameAsc(Long topicId);

    // Student read — only active subjects under an active topic.
    List<Subject> findByTopicIdAndTopicActiveTrueAndActiveTrueOrderByNameAsc(Long topicId);

    // Guards: topic-publish (needs an active subject) and subject last-active check.
    long countByTopicIdAndActiveTrue(Long topicId);
}
