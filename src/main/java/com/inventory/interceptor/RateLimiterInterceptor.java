package com.inventory.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterInterceptor.class);

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();
        String rateLimiterName = "apiRateLimiter";

        try {
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);

            if (rateLimiter.acquirePermission()) {
                logger.debug("RateLimiter '{}' permission granted for request: {}", rateLimiterName, requestPath);
                return true;
            } else {
                logger.warn("RateLimiter '{}' permission denied for request: {}", rateLimiterName, requestPath);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"RATE_LIMIT_EXCEEDED\", \"message\": \"Too many requests. Please try again later.\"}");
                return false;
            }
        } catch (Exception ex) {
            logger.error("Error in RateLimiter interceptor for request: {}", requestPath, ex);
            return true;
        }
    }
}
