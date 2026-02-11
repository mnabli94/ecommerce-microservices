package com.demo.auth.exception;

public class TokenCreationException extends RuntimeException {
    public TokenCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
