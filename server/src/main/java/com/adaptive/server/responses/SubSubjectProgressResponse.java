package com.adaptive.server.responses;

/**
 * One sub-subject plus the logged-in student's progress in it, for the Math Training page.
 * currentLevel is the raw progression level; difficultyLevel is the capped band (1–3)
 * questions are actually generated at; questionsSolved counts correct answers only.
 */
public class SubSubjectProgressResponse {

    private Long id;
    private String name;
    private int currentLevel;
    private long questionsSolved;
    private int difficultyLevel;

    public SubSubjectProgressResponse() {}

    public SubSubjectProgressResponse(Long id, String name, int currentLevel,
                                      long questionsSolved, int difficultyLevel) {
        this.id              = id;
        this.name            = name;
        this.currentLevel    = currentLevel;
        this.questionsSolved = questionsSolved;
        this.difficultyLevel = difficultyLevel;
    }

    public Long getId()             { return id; }
    public String getName()         { return name; }
    public int getCurrentLevel()    { return currentLevel; }
    public long getQuestionsSolved(){ return questionsSolved; }
    public int getDifficultyLevel() { return difficultyLevel; }

    public void setId(Long id)                          { this.id = id; }
    public void setName(String name)                    { this.name = name; }
    public void setCurrentLevel(int currentLevel)       { this.currentLevel = currentLevel; }
    public void setQuestionsSolved(long questionsSolved){ this.questionsSolved = questionsSolved; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }
}
