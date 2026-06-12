package com.adaptive.server.repository;

import com.adaptive.server.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {
    // Custom query example: Find progress by User ID
    List<StudentProgress> findByUserId(Long userId);
}
