package com.adaptive.server.service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    // Full name: English, Hebrew (א-ת) and Russian/Cyrillic (Ѐ-ӿ) letters,
    // at least two words separated by single spaces, up to 30 characters total.
    private static final String FULL_NAME_REGEX = "^(?=.{1,30}$)[A-Za-z\\u05D0-\\u05EA\\u0400-\\u04FF]+(?: [A-Za-z\\u05D0-\\u05EA\\u0400-\\u04FF]+)+$";
    // Password: 2-10 chars from the allowed set, and must contain at least one English letter.
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])[A-Za-z!0-9@#*]{2,10}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public boolean isValidFullName(String fullName) {
        return fullName != null && fullName.matches(FULL_NAME_REGEX);
    }

    public boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_REGEX);
    }

    public boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    public boolean isValidAge(Integer age) {
        return age != null && age >= 1 && age <= 120;
    }

    public boolean isValidGender(String gender) {
        return "male".equals(gender) || "female".equals(gender);
    }

}
