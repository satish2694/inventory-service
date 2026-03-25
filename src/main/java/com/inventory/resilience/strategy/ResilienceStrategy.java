package com.inventory.resilience.strategy;

import java.util.function.Supplier;

/**
 * Strategy pattern for applying resilience patterns to a supplier.
 * Follows Open/Closed Principle - open for extension, closed for modification.
 */
public interface ResilienceStrategy {
    <T> Supplier<T> decorate(Supplier<T> supplier);
    
    String getName();
}
