package com.inventory.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.monitoring.ResilienceMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for monitoring resilience patterns.
 * Follows Single Responsibility Principle - only handles HTTP requests.
 */
@RestController
@RequestMapping("/monitoring")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
public class MonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);

    private final ResilienceMonitoringService monitoringService;

    public MonitoringController(ResilienceMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/resilience4j/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResilience4jStatus() {
        logger.info("Fetching Resilience4j status");
        Map<String, Object> details = monitoringService.getAllDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "Resilience4j status retrieved"));
    }

    @GetMapping("/resilience4j/circuitbreaker")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCircuitBreakerDetails() {
        logger.info("Fetching CircuitBreaker details");
        Map<String, Object> details = monitoringService.getCircuitBreakerDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "CircuitBreaker details retrieved"));
    }

    @GetMapping("/resilience4j/bulkhead")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBulkheadDetails() {
        logger.info("Fetching Bulkhead details");
        Map<String, Object> details = monitoringService.getBulkheadDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "Bulkhead details retrieved"));
    }

    @GetMapping("/resilience4j/ratelimiter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimiterDetails() {
        logger.info("Fetching RateLimiter details");
        Map<String, Object> details = monitoringService.getRateLimiterDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "RateLimiter details retrieved"));
    }

    @GetMapping("/resilience4j/retry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRetryDetails() {
        logger.info("Fetching Retry details");
        Map<String, Object> details = monitoringService.getRetryDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "Retry details retrieved"));
    }

    @GetMapping("/resilience4j/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllResilience4jDetails() {
        logger.info("Fetching all Resilience4j details");
        Map<String, Object> details = monitoringService.getAllDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "All Resilience4j details retrieved"));
    }
}
