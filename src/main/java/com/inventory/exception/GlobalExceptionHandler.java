package com.inventory.exception;

import com.inventory.common.dto.ApiResponse;
import com.inventory.common.exception.ApplicationException;
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

/**
 * Global exception handler following Single Responsibility Principle.
 * Handles all exceptions and returns standardized ApiResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(
            CallNotPermittedException ex, WebRequest request) {
        String path = extractPath(request);
        logger.error("Circuit Breaker is OPEN for request: {}", path, ex);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service is temporarily unavailable. Circuit breaker is open.",
                        path
                ));
    }

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ApiResponse<Void>> handleBulkheadFull(
            BulkheadFullException ex, WebRequest request) {
        String path = extractPath(request);
        logger.warn("Bulkhead is full, request rejected for: {}", path);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too many concurrent requests. Please try again later.",
                        path
                ));
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(
            RequestNotPermitted ex, WebRequest request) {
        String path = extractPath(request);
        logger.warn("Rate limit exceeded for request: {}", path);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Rate limit exceeded. Please try again later.",
                        path
                ));
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationException(
            ApplicationException ex, WebRequest request) {
        String path = extractPath(request);
        logger.error("Application exception for request: {}", path, ex);
        
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(
                        ex.getStatusCode(),
                        ex.getMessage(),
                        path
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        String path = extractPath(request);
        logger.error("Unexpected error for request: {}", path, ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred. Please contact support.",
                        path
                ));
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
