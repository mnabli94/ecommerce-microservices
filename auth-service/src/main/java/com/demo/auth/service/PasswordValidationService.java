package com.demo.auth.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PasswordValidationService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    public void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String trimmedPassword = password.trim();

        if (trimmedPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (trimmedPassword.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password must not exceed " + MAX_LENGTH + " characters");
        }

        if (!PASSWORD_PATTERN.matcher(trimmedPassword).matches()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one digit, one lowercase letter, " +
                            "one uppercase letter, one special character (@#$%^&+=!), and no whitespace");
        }

        if (isCommonWeakPassword(trimmedPassword)) {
            throw new IllegalArgumentException("Password is too common or weak");
        }
    }

    private boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return lowerPassword.equals("password") ||
                lowerPassword.equals("12345678") ||
                lowerPassword.equals("qwerty123") ||
                lowerPassword.equals("admin123") ||
                lowerPassword.equals("user123");
    }
}