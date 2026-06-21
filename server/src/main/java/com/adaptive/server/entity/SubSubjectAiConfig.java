package com.adaptive.server.entity;

import javax.persistence.*;

/**
 * Optional AI-generator configuration for a sub-subject.
 *
 * The presence of a row for a given sub-subject means:
 * - Always use the AI question generator (regardless of the global {@code questions.ai.enabled} flag).
 * - No code-based fallback — if the AI service is unavailable the request fails visibly.
 *
 * Sub-subjects without a row follow the existing global flag logic.
 */
@Entity
@Table(name = "sub_subject_ai_config")
public class SubSubjectAiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "sub_subject_id", unique = true, nullable = false)
    private SubSubject subSubject;

    /** Descriptive topic string sent to the LLM. Differs from the display name shown in the UI. */
    @Column(name = "ai_topic", columnDefinition = "TEXT", nullable = false)
    private String aiTopic;

    /** When true, forces multiple-choice mode regardless of difficulty level. */
    @Column(name = "force_multiple_choice", nullable = false)
    private boolean forceMultipleChoice;

    public SubSubjectAiConfig() {
    }

    public SubSubjectAiConfig(SubSubject subSubject, String aiTopic, boolean forceMultipleChoice) {
        this.subSubject = subSubject;
        this.aiTopic = aiTopic;
        this.forceMultipleChoice = forceMultipleChoice;
    }

    public Long getId()                               { return id; }
    public void setId(Long id)                        { this.id = id; }

    public SubSubject getSubSubject()                 { return subSubject; }
    public void setSubSubject(SubSubject subSubject)  { this.subSubject = subSubject; }

    public String getAiTopic()                        { return aiTopic; }
    public void setAiTopic(String aiTopic)            { this.aiTopic = aiTopic; }

    public boolean isForceMultipleChoice()                        { return forceMultipleChoice; }
    public void setForceMultipleChoice(boolean forceMultipleChoice) { this.forceMultipleChoice = forceMultipleChoice; }
}
