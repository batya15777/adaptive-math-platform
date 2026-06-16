package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiQuestionRequest {

    private String topic;
    private String theme;
    private int difficulty;

    @JsonProperty("user_info")
    private UserInfo userInfo;

    private String language;

    @JsonProperty("multiple_choice")
    private boolean multipleChoice;

    public AiQuestionRequest() {}

    public AiQuestionRequest(String topic, String theme, int difficulty,
                             String userName, int userAge, String language, boolean multipleChoice) {
        this.topic         = topic;
        this.theme         = theme;
        this.difficulty    = difficulty;
        this.userInfo      = new UserInfo(userName, userAge);
        this.language      = language;
        this.multipleChoice = multipleChoice;
    }

    public String getTopic()             { return topic; }
    public String getTheme()             { return theme; }
    public int getDifficulty()           { return difficulty; }
    public UserInfo getUserInfo()        { return userInfo; }
    public String getLanguage()          { return language; }
    public boolean isMultipleChoice()    { return multipleChoice; }

    public static class UserInfo {
        private String name;
        private int age;

        public UserInfo() {}
        public UserInfo(String name, int age) { this.name = name; this.age = age; }

        public String getName() { return name; }
        public int getAge()     { return age; }
    }
}
