package com.inventory.common.exception;

public class ApplicationException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public ApplicationException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApplicationException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
