package com.adaptive.server.DTOs;

// Request to start a Galaxy Battle on a chosen topic (sub-subject). The student's level in that
// topic is resolved server-side; the client never sends it.
public class StartBattleRequest {
    private Long subSubjectId;
    private String language;

    public Long getSubSubjectId() { return subSubjectId; }
    public void setSubSubjectId(Long subSubjectId) { this.subSubjectId = subSubjectId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
