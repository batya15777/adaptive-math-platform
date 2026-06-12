package com.adaptive.server.repository;

import com.adaptive.server.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {

    List<StudentProgress> findByUserId(Long userId);

    Optional<StudentProgress> findByUserIdAndSubSubjectId(Long userId, Long subSubjectId);

    List<StudentProgress> findByUserIdAndIsActiveTrue(Long userId);
}

