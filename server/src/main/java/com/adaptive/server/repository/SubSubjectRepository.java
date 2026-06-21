package com.adaptive.server.repository;

import com.adaptive.server.entity.SubSubject;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
