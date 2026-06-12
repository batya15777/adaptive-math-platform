package com.adaptive.server.repository;

import com.adaptive.server.entity.SubSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubSubjectRepository extends JpaRepository<SubSubject, Long> {

    List<SubSubject> findBySubjectId(Long subjectId);

    Optional<SubSubject> findByNameAndSubjectId(String name, Long subjectId);
}
