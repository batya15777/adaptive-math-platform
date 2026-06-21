package com.adaptive.server.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "generated_questions")
public class GeneratedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_subject_id", nullable = false)
    private SubSubject subSubject;

    @Column(columnDefinition = "TEXT")
    private String expression;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @ElementCollection
    @CollectionTable(name = "generated_question_solution_steps", joinColumns = @JoinColumn(name = "generated_question_id"))
    @OrderColumn(name = "step_index")
    @Column(name = "step", columnDefinition = "TEXT")
    private List<String> solution = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "generated_question_options", joinColumns = @JoinColumn(name = "generated_question_id"))
    @OrderColumn(name = "option_index")
    @Column(name = "option_value")
    private List<String> options = new ArrayList<>();

    private String language;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @Column(name = "multiple_choice")
    private Boolean multipleChoice;

    public GeneratedQuestion() {
    }

    public GeneratedQuestion(SubSubject subSubject, String expression, String correctAnswer,
                             List<String> solution, List<String> options,
                             String language, Integer difficultyLevel, Boolean multipleChoice) {
        this.subSubject     = subSubject;
        this.expression     = expression;
        this.correctAnswer  = correctAnswer;
        this.solution       = solution != null ? solution : new ArrayList<>();
        this.options        = options != null ? options : new ArrayList<>();
        this.language       = language;
        this.difficultyLevel = difficultyLevel;
        this.multipleChoice = multipleChoice;
    }

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public SubSubject getSubSubject()                      { return subSubject; }
    public void setSubSubject(SubSubject subSubject)       { this.subSubject = subSubject; }

    public String getExpression()                          { return expression; }
    public void setExpression(String expression)           { this.expression = expression; }

    public String getCorrectAnswer()                       { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer)     { this.correctAnswer = correctAnswer; }

    public List<String> getSolution()                      { return solution; }
    public void setSolution(List<String> solution)         { this.solution = solution; }

    public List<String> getOptions()                       { return options; }
    public void setOptions(List<String> options)           { this.options = options; }

    public String getLanguage()                            { return language; }
    public void setLanguage(String language)               { this.language = language; }

    public Integer getDifficultyLevel()                    { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel){ this.difficultyLevel = difficultyLevel; }

    public Boolean getMultipleChoice()                     { return multipleChoice; }
    public void setMultipleChoice(Boolean multipleChoice)  { this.multipleChoice = multipleChoice; }
}
