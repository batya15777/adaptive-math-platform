package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TutorStudentContext {

    @JsonProperty("student_id")
    private Long studentId;

    private Integer age;

    @JsonProperty("current_level")
    private Integer currentLevel;

    @JsonProperty("weakness_type")
    private String weaknessType;

    public TutorStudentContext() {
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getWeaknessType() {
        return weaknessType;
    }

    public void setWeaknessType(String weaknessType) {
        this.weaknessType = weaknessType;
    }
}
