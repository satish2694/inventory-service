package com.inventory.common.exception;

import org.springframework.http.HttpStatus;

public class ServiceCallException extends ApplicationException {
    public ServiceCallException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_001");
    }

    public ServiceCallException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_001");
    }
}
