package com.inventory.api;

import com.inventory.exception.ErrorResponse;

/**
 * Sealed interface for API responses using Java 17 sealed classes.
 * Restricts implementations to specific response types for type safety.
 */
public sealed interface ApiResponse {
    record SuccessResponse(Object data, String message) implements ApiResponse {}
    record FailureResponse(ErrorResponse error) implements ApiResponse {}
}
