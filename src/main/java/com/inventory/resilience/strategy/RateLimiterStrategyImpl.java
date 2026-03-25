package com.inventory.resilience.strategy;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class RateLimiterStrategyImpl implements ResilienceStrategy {
    private final RateLimiterRegistry registry;
    private final String instanceName;

    public RateLimiterStrategyImpl(RateLimiterRegistry registry) {
        this.registry = registry;
        this.instanceName = "serviceBRateLimiter";
    }

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        RateLimiter rateLimiter = registry.rateLimiter(instanceName);
        return RateLimiter.decorateSupplier(rateLimiter, supplier);
    }

    @Override
    public String getName() {
        return "RateLimiter";
    }
}
