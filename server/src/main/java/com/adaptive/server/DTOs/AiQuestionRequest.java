package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiQuestionRequest {

    private String topic;
    private String theme;
    private int difficulty;

    @JsonProperty("difficulty_band")
    private String difficultyBand;

    private String language;

    @JsonProperty("multiple_choice")
    private boolean multipleChoice;

    // Cluster-derived adaptation hints. Null (and omitted) for students with no cohort.
    @JsonProperty("learning_profile")
    private LearningProfile learningProfile;

    public AiQuestionRequest() {}

    public AiQuestionRequest(String topic, String theme, int difficulty,
                             String language, boolean multipleChoice) {
        this.topic          = topic;
        this.theme          = theme;
        this.difficulty     = difficulty;
        this.language       = language;
        this.multipleChoice = multipleChoice;
    }

    public String getTopic()             { return topic; }
    public String getTheme()             { return theme; }
    public int getDifficulty()           { return difficulty; }
    public String getDifficultyBand()    { return difficultyBand; }
    public String getLanguage()          { return language; }
    public boolean isMultipleChoice()    { return multipleChoice; }
    public LearningProfile getLearningProfile() { return learningProfile; }

    public void setDifficultyBand(String difficultyBand) { this.difficultyBand = difficultyBand; }
    public void setLearningProfile(LearningProfile learningProfile) { this.learningProfile = learningProfile; }

    /**
     * Adaptive context forwarded to the LLM so it can gently target the student's
     * cohort weakness and calibrate to their mastery.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LearningProfile {

        @JsonProperty("cluster_label")
        private String clusterLabel;

        @JsonProperty("focus_skill")
        private String focusSkill;

        private String mastery; // "struggling" | "developing" | "strong"

        public LearningProfile() {}

        public LearningProfile(String clusterLabel, String focusSkill, String mastery) {
            this.clusterLabel = clusterLabel;
            this.focusSkill = focusSkill;
            this.mastery = mastery;
        }

        public String getClusterLabel() { return clusterLabel; }
        public String getFocusSkill()   { return focusSkill; }
        public String getMastery()      { return mastery; }
    }
}
