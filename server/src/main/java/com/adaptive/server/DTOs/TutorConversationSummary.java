package com.adaptive.server.DTOs;

import java.time.LocalDateTime;

/**
 * Projection for the tutor-chat history list: one row per question the student has
 * chatted about, with a preview (the question expression), the time of the last
 * message, and how many exchanges it holds. Populated by a grouped JPQL query.
 */
public interface TutorConversationSummary {

    Long getQuestionId();

    String getExpression();

    LocalDateTime getLastMessageAt();

    Long getMessageCount();
}
