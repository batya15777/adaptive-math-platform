package com.adaptive.server.entity;

import com.adaptive.server.entity.enums.QuestionStatus;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

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

    // Each element is one step, e.g. "3 * 4 = 12", ordered by index
    @ElementCollection
    @CollectionTable(name = "question_solution_steps", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "step_index")
    @Column(name = "step", columnDefinition = "TEXT")
    private List<String> solution = new ArrayList<>();

    // Each element is one answer option, e.g. "14", "11", "18", "9"
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_index")
    @Column(name = "option_value")
    private List<String> options = new ArrayList<>();

    private String language;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @Enumerated(EnumType.STRING)
    private QuestionStatus status;

    public Question() {
    }

    public Question(SubSubject subSubject, String expression, String correctAnswer,
                    List<String> solution, List<String> options,
                    String language, Integer difficultyLevel, QuestionStatus status) {
        this.subSubject = subSubject;
        this.expression = expression;
        this.correctAnswer = correctAnswer;
        this.solution = solution != null ? solution : new ArrayList<>();
        this.options = options != null ? options : new ArrayList<>();
        this.language = language;
        this.difficultyLevel = difficultyLevel;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SubSubject getSubSubject() { return subSubject; }
    public void setSubSubject(SubSubject subSubject) { this.subSubject = subSubject; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public List<String> getSolution() { return solution; }
    public void setSolution(List<String> solution) { this.solution = solution; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Question{" +
                "status=" + status +
                ", difficultyLevel=" + difficultyLevel +
                ", language='" + language + '\'' +
                ", options=" + options +
                ", solution=" + solution +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", expression='" + expression + '\'' +
                ", subSubject=" + subSubject +
                '}';
    }
}
