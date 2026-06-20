package com.adaptive.server.repository;

import com.adaptive.server.entity.StudentClusterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentClusterAssignmentRepository extends JpaRepository<StudentClusterAssignment, Long> {
    // Used both for upserting after a clustering run and (later) by the question
    // generator to read a student's cluster.
    Optional<StudentClusterAssignment> findByUserId(Long userId);
}
