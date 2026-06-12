package com.adaptive.server.utils;

public enum Errors {
    INVALID_REGISTRATION_REQUEST("Invalid registration request."),
    INVALID_FULL_NAME("Invalid full name."),
    INVALID_PASSWORD("Invalid password."),
    INVALID_EMAIL("Invalid email."),
    INVALID_AGE("Invalid age."),
    INVALID_GENDER("Invalid gender."),
    EMAIL_ALREADY_EXISTS("User with this email already exists."),
    VERIFICATION_NOT_FOUND("Pending registration not found."),
    INVALID_VERIFICATION_CODE("Invalid verification code."),
    VERIFICATION_CODE_EXPIRED("Verification code has expired."),
    VERIFICATION_EMAIL_SEND_FAILED("Unable to send verification email. Please try again."),
    USER_NOT_FOUND("User not found."),
    INVALID_CREDENTIALS("Invalid email or password."),


    SESSION_TOKEN_MISSING("Missing session token. Please log in first."),
    SESSION_TOKEN_INVALID("Invalid session token. Please log in again."),
    SESSION_TOKEN_EXPIRED("Session has expired. Please log in again."),
    ACCESS_DENIED_USER_MISMATCH("Access denied: token does not match the provided user ID."),
    SUB_SUBJECT_NOT_FOUND("Sub-subject not found.");


    private final String message;

    Errors(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
