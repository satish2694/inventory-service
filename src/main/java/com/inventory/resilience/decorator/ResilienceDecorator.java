package com.inventory.resilience.decorator;

import com.inventory.resilience.strategy.ResilienceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Decorator pattern implementation for composing multiple resilience strategies.
 * Follows Decorator and Composition patterns for flexible resilience application.
 */
@Component
public class ResilienceDecorator {
    private static final Logger logger = LoggerFactory.getLogger(ResilienceDecorator.class);

    public <T> Supplier<T> decorate(Supplier<T> supplier, List<ResilienceStrategy> strategies) {
        Supplier<T> decorated = supplier;
        
        for (ResilienceStrategy strategy : strategies) {
            decorated = strategy.decorate(decorated);
            logger.debug("Applied {} strategy", strategy.getName());
        }
        
        return decorated;
    }

    public <T> T execute(Supplier<T> supplier, List<ResilienceStrategy> strategies) {
        return decorate(supplier, strategies).get();
    }
}
