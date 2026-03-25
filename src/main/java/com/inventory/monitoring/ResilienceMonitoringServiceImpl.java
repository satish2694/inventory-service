package com.inventory.monitoring;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of resilience monitoring service.
 * Follows Single Responsibility Principle - only handles monitoring logic.
 */
@Service
public class ResilienceMonitoringServiceImpl implements ResilienceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceMonitoringServiceImpl.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ResilienceMonitoringServiceImpl(CircuitBreakerRegistry circuitBreakerRegistry,
                                         RetryRegistry retryRegistry,
                                         BulkheadRegistry bulkheadRegistry,
                                         RateLimiterRegistry rateLimiterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Map<String, Object> getCircuitBreakerDetails() {
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

        return details;
    }

    @Override
    public Map<String, Object> getBulkheadDetails() {
        logger.info("Fetching Bulkhead details");
        Map<String, Object> details = new HashMap<>();

        bulkheadRegistry.getAllBulkheads().forEach(bh -> {
            Map<String, Object> bhDetails = new HashMap<>();
            bhDetails.put("availableConcurrentCalls", bh.getMetrics().getAvailableConcurrentCalls());
            bhDetails.put("maxConcurrentCalls", bh.getBulkheadConfig().getMaxConcurrentCalls());
            bhDetails.put("queueCapacity", bh.getBulkheadConfig().getMaxWaitDuration().toMillis());
            details.put(bh.getName(), bhDetails);
        });

        return details;
    }

    @Override
    public Map<String, Object> getRateLimiterDetails() {
        logger.info("Fetching RateLimiter details");
        Map<String, Object> details = new HashMap<>();

        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            Map<String, Object> rlDetails = new HashMap<>();
            rlDetails.put("availablePermissions", rl.getMetrics().getAvailablePermissions());
            rlDetails.put("limitForPeriod", rl.getRateLimiterConfig().getLimitForPeriod());
            rlDetails.put("limitRefreshPeriod", rl.getRateLimiterConfig().getLimitRefreshPeriod().toString());
            details.put(rl.getName(), rlDetails);
        });

        return details;
    }

    @Override
    public Map<String, Object> getRetryDetails() {
        logger.info("Fetching Retry details");
        Map<String, Object> details = new HashMap<>();

        retryRegistry.getAllRetries().forEach(retry -> {
            Map<String, Object> retryDetails = new HashMap<>();
            retryDetails.put("maxAttempts", retry.getRetryConfig().getMaxAttempts());
            var intervalFunction = retry.getRetryConfig().getIntervalFunction();
            retryDetails.put("intervalFunction", intervalFunction != null ? intervalFunction.toString() : "default");
            details.put(retry.getName(), retryDetails);
        });

        return details;
    }

    @Override
    public Map<String, Object> getAllDetails() {
        logger.info("Fetching all Resilience4j details");
        Map<String, Object> allDetails = new HashMap<>();

        allDetails.put("circuitBreakers", getCircuitBreakerDetails());
        allDetails.put("bulkheads", getBulkheadDetails());
        allDetails.put("rateLimiters", getRateLimiterDetails());
        allDetails.put("retries", getRetryDetails());

        return allDetails;
    }
}
