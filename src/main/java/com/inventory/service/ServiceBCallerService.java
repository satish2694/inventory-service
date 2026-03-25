package com.inventory.service;

import com.inventory.client.ServiceBFeignClient;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class ServiceBCallerService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBCallerService.class);

    @Autowired
    private ServiceBFeignClient serviceBFeignClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    public String callServiceB(String cookieValue) {
        logger.info("Initiating call to ServiceB with cookie value: {}", cookieValue);
        long startTime = System.currentTimeMillis();

        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("serviceBCircuitBreaker");
            Retry retry = retryRegistry.retry("serviceBRetry");
            Bulkhead bulkhead = bulkheadRegistry.bulkhead("serviceBBulkhead");
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("serviceBRateLimiter");

            Supplier<String> supplier = () -> {
                logger.debug("Executing ServiceB call with Feign client");
                return serviceBFeignClient.displayMessage(cookieValue);
            };

            Supplier<String> rateLimited = RateLimiter.decorateSupplier(rateLimiter, supplier);
            Supplier<String> circuitBreakerDecorated = CircuitBreaker.decorateSupplier(circuitBreaker, rateLimited);
            Supplier<String> retryDecorated = Retry.decorateSupplier(retry, circuitBreakerDecorated);
            Supplier<String> bulkheadDecorated = Bulkhead.decorateSupplier(bulkhead, retryDecorated);

            String result = bulkheadDecorated.get();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("ServiceB call completed successfully in {} ms", duration);
            return result;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("ServiceB call failed after {} ms: {}", duration, ex.getMessage(), ex);
            throw new RuntimeException("Failed to call ServiceB: " + ex.getMessage(), ex);
        }
    }

    public String callServiceBWithFallback(String cookieValue) {
        logger.info("Initiating call to ServiceB with fallback strategy");

        try {
            return callServiceB(cookieValue);
        } catch (Exception ex) {
            logger.warn("ServiceB call failed, returning fallback response: {}", ex.getMessage());
            return "Fallback response: Service temporarily unavailable. Please try again later.";
        }
    }
}
