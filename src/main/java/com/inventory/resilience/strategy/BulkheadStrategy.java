package com.inventory.resilience.strategy;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class BulkheadStrategy implements ResilienceStrategy {
    private final BulkheadRegistry registry;
    private final String instanceName;

    public BulkheadStrategy(BulkheadRegistry registry) {
        this.registry = registry;
        this.instanceName = "serviceBBulkhead";
    }

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        Bulkhead bulkhead = registry.bulkhead(instanceName);
        return Bulkhead.decorateSupplier(bulkhead, supplier);
    }

    @Override
    public String getName() {
        return "Bulkhead";
    }
}
