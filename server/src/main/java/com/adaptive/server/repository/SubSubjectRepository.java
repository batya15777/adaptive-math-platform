package com.adaptive.server.repository;

import com.adaptive.server.entity.SubSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubSubjectRepository extends JpaRepository<SubSubject, Long> {

    public List<SubSubject> findBySubjectId(Long subjectId);

    SubSubject findByName(String name);

    SubSubject findByNameAndSubject_Name(String name, String subjectName);

    public Optional<SubSubject> findByNameAndSubjectId(String name, Long subjectId);

    long countBySubjectId(Long subjectId);

    // Admin list — all sub-subjects of a subject (active + draft), ordered by name.
    List<SubSubject> findBySubjectIdOrderByNameAsc(Long subjectId);

    // Student read — only active sub-subjects under an active subject under an active topic.
    // Explicit @Query (instead of a long derived name) to keep the nested-active filter unambiguous.
    @Query("SELECT ss FROM SubSubject ss "
            + "WHERE ss.subject.id = :subjectId "
            + "AND ss.active = true AND ss.subject.active = true AND ss.subject.topic.active = true")
    List<SubSubject> findActiveForStudentBySubjectId(@Param("subjectId") Long subjectId);

    // Guards (subject-publish needs an active sub-subject; sub-subject last-active check).
    long countBySubjectIdAndActiveTrue(Long subjectId);
}
