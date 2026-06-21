package com.adaptive.server.repository;

import com.adaptive.server.DTOs.TutorConversationSummary;
import com.adaptive.server.entity.TutorChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorChatMessageRepository extends JpaRepository<TutorChatMessage, Long> {

    List<TutorChatMessage> findByUserIdAndQuestionIdOrderByCreatedAtAsc(Long userId, Long questionId);

    // One summary row per question the student chatted about, newest activity first.
    @Query("SELECT m.question.id AS questionId, m.question.expression AS expression, " +
            "MAX(m.createdAt) AS lastMessageAt, COUNT(m) AS messageCount " +
            "FROM TutorChatMessage m " +
            "WHERE m.user.id = :userId " +
            "GROUP BY m.question.id, m.question.expression " +
            "ORDER BY MAX(m.createdAt) DESC")
    List<TutorConversationSummary> findConversationSummaries(@Param("userId") Long userId);
}
