package com.demo.auth.exception;

public class UsernameAlreadyExistsException extends IllegalArgumentException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }

    public UsernameAlreadyExistsException(String username, Throwable cause) {
        super("Username already exists: " + username, cause);
    }
}
