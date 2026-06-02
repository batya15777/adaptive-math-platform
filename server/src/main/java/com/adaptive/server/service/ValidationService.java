package com.adaptive.server.service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    private static final String USERNAME_REGEX = "^[A-Za-z]{2,10}$";
    private static final String PASSWORD_REGEX = "^[A-Za-z!0-9@#*]{2,10}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public boolean isValidUsername(String username) {
        return username != null && username.matches(USERNAME_REGEX);
    }

    public boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_REGEX);
    }

    public boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    public boolean isValidGender(String gender) {
        return "male".equals(gender) || "female".equals(gender);
    }

}
