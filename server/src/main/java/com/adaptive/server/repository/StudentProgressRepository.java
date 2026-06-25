package com.adaptive.server.repository;

import com.adaptive.server.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {

    List<StudentProgress> findByUserId(Long userId);

    // Defensive against legacy duplicate rows: historically there was no unique constraint on
    // (user, sub_subject), so a student could end up with >1 progress row for the same
    // sub-subject. A plain Optional-returning derived query then throws NonUniqueResultException
    // (HTTP 500) on those students. We instead fetch all matches ordered deterministically and
    // return the earliest, so reads never crash. (Run the dedupe + unique constraint to fix the
    // data at the source.)
    default Optional<StudentProgress> findByUserIdAndSubSubjectId(Long userId, Long subSubjectId) {
        List<StudentProgress> rows = findAllByUserIdAndSubSubjectIdOrderByCurrentLevelDescIdDesc(userId, subSubjectId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // Ordered so the most-advanced (and, as a tiebreak, the newest) row wins — never lose a
    // student's progress when legacy duplicates exist.
    List<StudentProgress> findAllByUserIdAndSubSubjectIdOrderByCurrentLevelDescIdDesc(Long userId, Long subSubjectId);

    List<StudentProgress> findByUserIdAndIsActiveTrue(Long userId);
}

