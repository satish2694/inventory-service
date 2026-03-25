package com.inventory.monitoring;

import java.util.Map;

/**
 * Interface for monitoring resilience components.
 * Follows Dependency Inversion Principle for flexible monitoring implementations.
 */
public interface ResilienceMonitoringService {
    Map<String, Object> getCircuitBreakerDetails();
    
    Map<String, Object> getBulkheadDetails();
    
    Map<String, Object> getRateLimiterDetails();
    
    Map<String, Object> getRetryDetails();
    
    Map<String, Object> getAllDetails();
}
