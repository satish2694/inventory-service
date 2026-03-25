package com.inventory.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({CallNotPermittedException.class, BulkheadFullException.class, RequestNotPermitted.class, Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        LocalDateTime timestamp = LocalDateTime.now();

        // Java 21 Pattern Matching for instanceof
        ErrorResponse errorResponse;
        HttpStatus status;

        if (ex instanceof CallNotPermittedException e) {
            logger.error("Circuit Breaker is OPEN for request: {}", path, e);
            errorResponse = ErrorResponse.builder()
                    .timestamp(timestamp)
                    .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .error("SERVICE_UNAVAILABLE")
                    .message("Service is temporarily unavailable. Circuit breaker is open.")
                    .details(e.getMessage())
                    .path(path)
                    .build();
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof BulkheadFullException e) {
            logger.warn("Bulkhead is full, request rejected for: {}", path);
            errorResponse = ErrorResponse.builder()
                    .timestamp(timestamp)
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("BULKHEAD_FULL")
                    .message("Too many concurrent requests. Please try again later.")
                    .details(e.getMessage())
                    .path(path)
                    .build();
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if (ex instanceof RequestNotPermitted e) {
            logger.warn("Rate limit exceeded for request: {}", path);
            errorResponse = ErrorResponse.builder()
                    .timestamp(timestamp)
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("RATE_LIMIT_EXCEEDED")
                    .message("Rate limit exceeded. Please try again later.")
                    .details(e.getMessage())
                    .path(path)
                    .build();
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else {
            logger.error("Unexpected error for request: {}", path, ex);
            errorResponse = ErrorResponse.builder()
                    .timestamp(timestamp)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .error("INTERNAL_SERVER_ERROR")
                    .message("An unexpected error occurred. Please contact support.")
                    .details(ex.getMessage())
                    .path(path)
                    .build();
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(errorResponse, status);
    }
}
