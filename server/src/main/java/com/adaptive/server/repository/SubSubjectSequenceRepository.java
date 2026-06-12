package com.adaptive.server.repository;

import com.adaptive.server.entity.SubSubjectSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubSubjectSequenceRepository extends JpaRepository<SubSubjectSequence, Long> {

    Optional<SubSubjectSequence> findByCurrentSubjectId(Long currentSubjectId);
}
