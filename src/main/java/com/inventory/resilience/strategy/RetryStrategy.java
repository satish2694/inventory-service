package com.inventory.resilience.strategy;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class RetryStrategy implements ResilienceStrategy {
    private final RetryRegistry registry;
    private final String instanceName;

    public RetryStrategy(RetryRegistry registry) {
        this.registry = registry;
        this.instanceName = "serviceBRetry";
    }

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        Retry retry = registry.retry(instanceName);
        return Retry.decorateSupplier(retry, supplier);
    }

    @Override
    public String getName() {
        return "Retry";
    }
}
