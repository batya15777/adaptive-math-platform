package com.adaptive.server.DTOs;

public class RegisterRequest {


    private String fullName;

    private String password;

    private String email;

    private Integer age;

    private String gender;


    public RegisterRequest(String fullName, String password, String email, Integer age, String gender) {
        this.fullName = fullName;
        this.password = password;
        this.email = email;
        this.age = age;
        this.gender = gender;
    }
    public RegisterRequest(){}

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
