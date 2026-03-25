package com.inventory.controller;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/monitoring")
public class MonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @GetMapping("/resilience4j/status")
    public ResponseEntity<Map<String, Object>> getResilience4jStatus() {
        logger.info("Fetching Resilience4j status");
        Map<String, Object> status = new HashMap<>();

        Map<String, String> circuitBreakerStatus = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb ->
                circuitBreakerStatus.put(cb.getName(), cb.getState().toString())
        );
        status.put("circuitBreakers", circuitBreakerStatus);

        Map<String, Map<String, Object>> bulkheadStatus = new HashMap<>();
        bulkheadRegistry.getAllBulkheads().forEach(bh -> {
            Map<String, Object> bhMetrics = new HashMap<>();
            bhMetrics.put("availableConcurrentCalls", bh.getMetrics().getAvailableConcurrentCalls());
            bhMetrics.put("maxConcurrentCalls", bh.getBulkheadConfig().getMaxConcurrentCalls());
            bulkheadStatus.put(bh.getName(), bhMetrics);
        });
        status.put("bulkheads", bulkheadStatus);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/resilience4j/circuitbreaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerDetails() {
        logger.info("Fetching CircuitBreaker details");
        Map<String, Object> details = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", cb.getState().toString());
            cbDetails.put("failureRate", cb.getMetrics().getFailureRate());
            cbDetails.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbDetails.put("numberOfBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            cbDetails.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbDetails.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            details.put(cb.getName(), cbDetails);
        });

        return ResponseEntity.ok(details);
    }

    @GetMapping("/resilience4j/bulkhead")
    public ResponseEntity<Map<String, Object>> getBulkheadDetails() {
        logger.info("Fetching Bulkhead details");
        Map<String, Object> details = new HashMap<>();

        bulkheadRegistry.getAllBulkheads().forEach(bh -> {
            Map<String, Object> bhDetails = new HashMap<>();
            bhDetails.put("availableConcurrentCalls", bh.getMetrics().getAvailableConcurrentCalls());
            bhDetails.put("maxConcurrentCalls", bh.getBulkheadConfig().getMaxConcurrentCalls());
            bhDetails.put("queueCapacity", bh.getBulkheadConfig().getMaxWaitDuration().toMillis());
            details.put(bh.getName(), bhDetails);
        });

        return ResponseEntity.ok(details);
    }
}
