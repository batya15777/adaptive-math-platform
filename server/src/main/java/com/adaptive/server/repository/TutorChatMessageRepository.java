package com.adaptive.server.repository;

import com.adaptive.server.entity.TutorChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorChatMessageRepository extends JpaRepository<TutorChatMessage, Long> {

    List<TutorChatMessage> findByUserIdAndQuestionIdOrderByCreatedAtAsc(Long userId, Long questionId);
}
