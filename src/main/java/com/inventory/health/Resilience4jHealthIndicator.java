package com.inventory.health;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class Resilience4jHealthIndicator implements HealthIndicator {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            String status = cb.getState().toString();
            builder.withDetail("circuitbreaker_" + cb.getName(), status);
        });

        bulkheadRegistry.getAllBulkheads().forEach(bh -> {
            int availableConcurrentCalls = bh.getMetrics().getAvailableConcurrentCalls();
            int maxConcurrentCalls = bh.getBulkheadConfig().getMaxConcurrentCalls();
            builder.withDetail("bulkhead_" + bh.getName(),
                    String.format("%d/%d", availableConcurrentCalls, maxConcurrentCalls));
        });

        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            int availablePermissions = rl.getMetrics().getAvailablePermissions();
            int limitForPeriod = rl.getRateLimiterConfig().getLimitForPeriod();
            builder.withDetail("ratelimiter_" + rl.getName(),
                    String.format("%d/%d", availablePermissions, limitForPeriod));
        });

        return builder.up().build();
    }
}
