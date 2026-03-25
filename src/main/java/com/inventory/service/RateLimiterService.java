package com.inventory.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    public boolean allowRequest(String rateLimiterName) {
        try {
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);
            boolean allowed = rateLimiter.acquirePermission();
            
            if (allowed) {
                logger.debug("RateLimiter '{}' permission granted", rateLimiterName);
            } else {
                logger.warn("RateLimiter '{}' permission denied - limit exceeded", rateLimiterName);
            }
            
            return allowed;
        } catch (Exception ex) {
            logger.error("Error checking rate limit for '{}': {}", rateLimiterName, ex.getMessage());
            return true;
        }
    }

    public Map<String, Object> getRateLimiterMetrics(String rateLimiterName) {
        try {
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);
            Map<String, Object> metrics = new HashMap<>();
            
            metrics.put("name", rateLimiterName);
            metrics.put("limitForPeriod", rateLimiter.getRateLimiterConfig().getLimitForPeriod());
            metrics.put("limitRefreshPeriod", rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().toString());
            metrics.put("timeoutDuration", rateLimiter.getRateLimiterConfig().getTimeoutDuration().toString());
            metrics.put("availablePermissions", rateLimiter.getMetrics().getAvailablePermissions());
            metrics.put("numberOfWaitingThreads", rateLimiter.getMetrics().getNumberOfWaitingThreads());
            
            return metrics;
        } catch (Exception ex) {
            logger.error("Error fetching metrics for RateLimiter '{}': {}", rateLimiterName, ex.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, Object>> getAllRateLimiterMetrics() {
        Map<String, Map<String, Object>> allMetrics = new HashMap<>();
        
        rateLimiterRegistry.getAllRateLimiters().forEach(rateLimiter -> {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("limitForPeriod", rateLimiter.getRateLimiterConfig().getLimitForPeriod());
            metrics.put("limitRefreshPeriod", rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().toString());
            metrics.put("timeoutDuration", rateLimiter.getRateLimiterConfig().getTimeoutDuration().toString());
            metrics.put("availablePermissions", rateLimiter.getMetrics().getAvailablePermissions());
            metrics.put("numberOfWaitingThreads", rateLimiter.getMetrics().getNumberOfWaitingThreads());
            
            allMetrics.put(rateLimiter.getName(), metrics);
        });
        
        return allMetrics;
    }
}
