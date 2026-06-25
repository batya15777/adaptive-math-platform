package com.adaptive.server.DTOs;

// Records the outcome of a battle (history + badge). Never changes level progression.
public class FinishBattleRequest {
    private Long subSubjectId;
    private Integer level;
    private boolean won;

    public Long getSubSubjectId() { return subSubjectId; }
    public void setSubSubjectId(Long subSubjectId) { this.subSubjectId = subSubjectId; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }
}
