package com.inventory.resilience.strategy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class CircuitBreakerStrategy implements ResilienceStrategy {
    private final CircuitBreakerRegistry registry;
    private final String instanceName;

    public CircuitBreakerStrategy(CircuitBreakerRegistry registry) {
        this.registry = registry;
        this.instanceName = "serviceBCircuitBreaker";
    }

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker(instanceName);
        return CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
    }

    @Override
    public String getName() {
        return "CircuitBreaker";
    }
}
