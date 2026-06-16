package com.adaptive.server.repository;

import com.adaptive.server.entity.QuestionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface QuestionArchiveRepository extends JpaRepository<QuestionArchive, Long> {

    /**
     * Returns the set of expression strings that a user has already been shown
     * for a given sub-subject.
     *
     * <p>Used by {@code LevelManagerService.getBonusQuestion()} to avoid generating
     * a bonus question whose expression the student has already seen.  Selecting only
     * the {@code expression} column (not full entities) keeps the query lightweight
     * even for users with a long history.
     *
     * @param userId       the user's primary key
     * @param subSubjectId the sub-subject's primary key
     * @return a {@link Set} of previously-seen expression strings; empty if none
     */
    @Query("SELECT a.expression FROM QuestionArchive a " +
           "WHERE a.user.id = :userId AND a.subSubject.id = :subSubjectId")
    Set<String> findSeenExpressionsByUserAndSubSubject(
            @Param("userId")       Long userId,
            @Param("subSubjectId") Long subSubjectId);
}
