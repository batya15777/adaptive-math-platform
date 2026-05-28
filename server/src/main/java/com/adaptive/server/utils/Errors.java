package com.adaptive.server.utils;

public enum Errors {
    USER_ALREADY_EXISTS("User with the given username already exists."),
    INVALID_CREDENTIALS("Invalid username or password or email or email."),
    USER_NOT_FOUND("User not found.");


    private final String message;

    Errors(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
