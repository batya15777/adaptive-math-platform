package com.adaptive.server.utils;

public enum Errors {
    INVALID_REGISTRATION_REQUEST("Invalid registration request."),
    INVALID_USERNAME("Invalid username."),
    INVALID_PASSWORD("Invalid password."),
    INVALID_EMAIL("Invalid email."),
    INVALID_GENDER("Invalid gender."),
    EMAIL_ALREADY_EXISTS("User with this email already exists."),
    USER_NOT_FOUND("User not found."),
    INVALID_CREDENTIALS("Invalid email or password.");


    private final String message;

    Errors(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
