package com.adaptive.server.DTOs;

public class LoginRequest {
    private String email;
    private String password;
    // "Remember me": when true the session lives ~30 days and the cookie persists across
    // browser restarts; when false it's a session cookie that expires when the browser closes.
    private boolean remember;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public LoginRequest() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRemember() {
        return remember;
    }

    public void setRemember(boolean remember) {
        this.remember = remember;
    }
}
