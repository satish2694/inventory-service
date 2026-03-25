package com.inventory.common.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends ApplicationException {
    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED.value(), "AUTH_001");
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.UNAUTHORIZED.value(), "AUTH_001");
    }
}
