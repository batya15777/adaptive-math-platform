package com.adaptive.server.repository;

import com.adaptive.server.entity.SubjectSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectSequenceRepository extends JpaRepository<SubjectSequence, Long> {

    Optional<SubjectSequence> findByCurrentSubjectId(Long currentSubjectId);
}
