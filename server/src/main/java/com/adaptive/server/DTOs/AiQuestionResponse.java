package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** Maps the JSON response from the Python AI microservice. */
public class AiQuestionResponse {

    @JsonProperty("question_text")
    private String questionText;

    @JsonProperty("correct_answer")
    private String correctAnswer;

    private List<String> options = new ArrayList<>();

    @JsonProperty("step_by_step_solution")
    private List<String> stepByStepSolution = new ArrayList<>();

    @JsonProperty("difficulty_level")
    private int difficultyLevel;

    private String language;

    public AiQuestionResponse() {}

    public String getQuestionText()             { return questionText; }
    public String getCorrectAnswer()            { return correctAnswer; }
    public List<String> getOptions()            { return options != null ? options : new ArrayList<>(); }
    public List<String> getStepByStepSolution() { return stepByStepSolution != null ? stepByStepSolution : new ArrayList<>(); }
    public int getDifficultyLevel()             { return difficultyLevel; }
    public String getLanguage()                 { return language; }
}
