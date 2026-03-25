package com.inventory.service;

import com.inventory.client.ServiceBFeignClient;
import com.inventory.common.exception.ServiceCallException;
import com.inventory.resilience.decorator.ResilienceDecorator;
import com.inventory.resilience.strategy.BulkheadStrategy;
import com.inventory.resilience.strategy.CircuitBreakerStrategy;
import com.inventory.resilience.strategy.ResilienceStrategy;
import com.inventory.resilience.strategy.RetryStrategy;
import com.inventory.resilience.strategy.RateLimiterStrategyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Service for calling ServiceB with resilience patterns.
 * Follows Single Responsibility Principle - only handles service calls.
 * Uses Strategy and Decorator patterns for flexible resilience application.
 */
@Service
public class ServiceBCallerService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBCallerService.class);
    private static final String FALLBACK_MESSAGE = "Fallback response: Service temporarily unavailable. Please try again later.";

    private final ServiceBFeignClient serviceBFeignClient;
    private final ResilienceDecorator resilienceDecorator;
    private final List<ResilienceStrategy> strategies;

    public ServiceBCallerService(ServiceBFeignClient serviceBFeignClient,
                               ResilienceDecorator resilienceDecorator,
                               CircuitBreakerStrategy circuitBreakerStrategy,
                               RetryStrategy retryStrategy,
                               BulkheadStrategy bulkheadStrategy,
                               RateLimiterStrategyImpl rateLimiterStrategy) {
        this.serviceBFeignClient = serviceBFeignClient;
        this.resilienceDecorator = resilienceDecorator;
        this.strategies = Arrays.asList(
                rateLimiterStrategy,
                circuitBreakerStrategy,
                retryStrategy,
                bulkheadStrategy
        );
    }

    public String callServiceB(String cookieValue) {
        logger.info("Initiating call to ServiceB with cookie value: {}", cookieValue);
        long startTime = System.currentTimeMillis();

        try {
            Supplier<String> supplier = () -> {
                logger.debug("Executing ServiceB call with Feign client");
                return serviceBFeignClient.displayMessage(cookieValue);
            };

            String result = resilienceDecorator.execute(supplier, strategies);
            long duration = System.currentTimeMillis() - startTime;
            logger.info("ServiceB call completed successfully in {} ms", duration);
            return result;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("ServiceB call failed after {} ms: {}", duration, ex.getMessage(), ex);
            throw new ServiceCallException("Failed to call ServiceB: " + ex.getMessage(), ex);
        }
    }

    public String callServiceBWithFallback(String cookieValue) {
        logger.info("Initiating call to ServiceB with fallback strategy");

        try {
            return callServiceB(cookieValue);
        } catch (Exception ex) {
            logger.warn("ServiceB call failed, returning fallback response: {}", ex.getMessage());
            return FALLBACK_MESSAGE;
        }
    }
}
