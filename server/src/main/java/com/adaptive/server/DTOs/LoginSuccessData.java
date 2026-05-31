package com.adaptive.server.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginSuccessData {
    //לקחתי השראה מפרוייקט כיתתי -
    // העברת טוקן אל CONTROLLER ומחיקתו לפני שליחה אל צד לקוח, זה בעצם אובייקט משלוח זמני

    private UserResponseDTO user;
    private String token; // נשמר פה זמנית רק עד הCONTROLLER

    public LoginSuccessData(UserResponseDTO user, String token) {
        this.user = user;
        this.token = token;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public void setUser(UserResponseDTO user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
