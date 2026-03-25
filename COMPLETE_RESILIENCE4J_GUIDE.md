# Complete Production-Grade Resilience4J Implementation Guide

## Overview
This microservice implements a comprehensive, production-grade resilience solution using Resilience4J with five key patterns: CircuitBreaker, Retry, Bulkhead, TimeLimiter, and RateLimiter.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Request                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  RateLimiter Interceptor       │ (API-level rate limiting)
        │  (200 req/min)                 │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Controller Layer              │
        │  (Request handling)            │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Service Layer                 │
        │  (Business logic)              │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────────────────┐
        │         Resilience4J Decorator Chain               │
        │  ┌──────────────────────────────────────────────┐  │
        │  │ 1. RateLimiter (50 req/min to ServiceB)     │  │
        │  │ 2. CircuitBreaker (60% failure threshold)   │  │
        │  │ 3. Retry (4 attempts, 1s wait)             │  │
        │  │ 4. Bulkhead (5 concurrent calls)           │  │
        │  │ 5. TimeLimiter (3s timeout)                │  │
        │  └──────────────────────────────────────────────┘  │
        └────────────────┬───────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Feign Client                  │
        │  (ServiceB call)               │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Global Exception Handler      │
        │  (Error handling & logging)    │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Response to Client            │
        │  (Success or Error)            │
        └────────────────────────────────┘
```

## Resilience Patterns

### 1. RateLimiter Pattern
**Purpose**: Limit the number of requests within a time period

**Configuration**:
- API Level: 200 requests/minute
- Service Level: 50 requests/minute
- Heavy Operations: 10 requests/minute

**Implementation**:
- Interceptor for API-level limiting
- Decorator for service-level limiting
- Custom service for metrics

**Benefits**:
- Prevents service overload
- Fair resource allocation
- Predictable performance

### 2. CircuitBreaker Pattern
**Purpose**: Prevent cascading failures by stopping requests to failing services

**Configuration**:
- Failure Rate Threshold: 60%
- Slow Call Rate Threshold: 50%
- Wait Duration in Open State: 15 seconds
- Permitted Calls in Half-Open: 3

**States**:
- CLOSED: Normal operation
- OPEN: Rejecting requests
- HALF_OPEN: Testing recovery

**Benefits**:
- Fast failure detection
- Automatic recovery
- Reduced resource waste

### 3. Retry Pattern
**Purpose**: Automatically retry failed requests with exponential backoff

**Configuration**:
- Max Attempts: 4
- Wait Duration: 1 second
- Retry on: All exceptions except NullPointerException

**Benefits**:
- Handles transient failures
- Improves reliability
- Reduces manual intervention

### 4. Bulkhead Pattern
**Purpose**: Limit concurrent requests to prevent resource exhaustion

**Configuration**:
- Max Concurrent Calls: 5
- Max Wait Duration: 1 second

**Benefits**:
- Resource isolation
- Prevents thread pool exhaustion
- Predictable latency

### 5. TimeLimiter Pattern
**Purpose**: Prevent long-running requests from blocking resources

**Configuration**:
- Timeout Duration: 3 seconds

**Benefits**:
- Prevents hanging requests
- Improves responsiveness
- Protects system resources

## API Endpoints

### Service Endpoints
```
GET /inventory-service/displayMessage
  - Returns: Inventory service message
  - Rate Limit: 200/min (API level)

GET /inventory-service/callServiceB?cookie=dark
  - Calls ServiceB with all resilience patterns
  - Rate Limit: 200/min (API level) + 50/min (service level)
  - Returns: Response from ServiceB or error

GET /inventory-service/callServiceBWithFallback?cookie=dark
  - Calls ServiceB with fallback strategy
  - Returns: ServiceB response or fallback message
```

### Monitoring Endpoints
```
GET /monitoring/resilience4j/status
  - Returns: Overall status of all components

GET /monitoring/resilience4j/circuitbreaker
  - Returns: CircuitBreaker metrics and state

GET /monitoring/resilience4j/bulkhead
  - Returns: Bulkhead metrics

GET /monitoring/resilience4j/ratelimiter
  - Returns: RateLimiter metrics

GET /monitoring/resilience4j/ratelimiter/{name}
  - Returns: Specific RateLimiter metrics

GET /monitoring/resilience4j/all
  - Returns: All Resilience4J components details

GET /actuator/health
  - Returns: Application health including Resilience4J status

GET /actuator/metrics
  - Returns: Detailed metrics
```

## Exception Handling

### Exception Hierarchy
```
Exception
├── CallNotPermittedException (503 - Circuit Breaker Open)
├── RetryExhaustedException (503 - Retry Failed)
├── BulkheadFullException (429 - Bulkhead Full)
├── RequestNotPermitted (429 - Rate Limit Exceeded)
├── TimeLimiterException (504 - Timeout)
└── Generic Exception (500 - Internal Error)
```

### Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:45.123456",
  "status": 503,
  "error": "SERVICE_UNAVAILABLE",
  "message": "Service is temporarily unavailable. Circuit breaker is open.",
  "details": "Exception message details",
  "path": "/inventory-service/callServiceB",
  "traceId": "optional-trace-id"
}
```

## Configuration

### application.yaml
All configurations are externalized in `application.yaml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      serviceBCircuitBreaker:
        failureRateThreshold: 60
        waitDurationInOpenState: 15s
        
  retry:
    instances:
      serviceBRetry:
        maxAttempts: 4
        waitDuration: 1000
        
  bulkhead:
    instances:
      serviceBBulkhead:
        maxConcurrentCalls: 5
        maxWaitDuration: 1000ms
        
  timelimiter:
    instances:
      serviceBTimeLimiter:
        timeoutDuration: 3s
        
  ratelimiter:
    instances:
      serviceBRateLimiter:
        limitForPeriod: 50
        limitRefreshPeriod: 1m
      apiRateLimiter:
        limitForPeriod: 200
        limitRefreshPeriod: 1m
```

## Monitoring and Observability

### Health Indicators
- Custom Resilience4j health indicator
- Shows status of all components
- Available via `/actuator/health`

### Metrics
- Micrometer integration
- Detailed metrics for each pattern
- Available via `/actuator/metrics`

### Logging
- Comprehensive logging at all levels
- Event listeners for state changes
- Detailed error logging

### Event Listeners
- CircuitBreaker state transitions
- Retry attempts
- Bulkhead rejections
- RateLimiter violations
- TimeLimiter timeouts

## Testing Scenarios

### Test 1: CircuitBreaker
1. Call `/inventory-service/callServiceB` multiple times
2. Trigger failures to exceed 60% failure rate
3. Circuit breaker opens, returns 503
4. After 15 seconds, transitions to HALF_OPEN
5. Successful calls transition back to CLOSED

### Test 2: RateLimiter
1. Send 201 requests to `/inventory-service/displayMessage` in quick succession
2. After 200 requests, subsequent requests return 429
3. Monitor via `/monitoring/resilience4j/ratelimiter`

### Test 3: Bulkhead
1. Send 6 concurrent requests to `/inventory-service/callServiceB`
2. 6th request is rejected with 429
3. Monitor via `/monitoring/resilience4j/bulkhead`

### Test 4: Retry
1. Call `/inventory-service/callServiceB` when service is temporarily unavailable
2. Retry mechanism automatically retries up to 4 times
3. Check logs for retry attempts

### Test 5: TimeLimiter
1. Call `/inventory-service/callServiceB` with a slow service
2. After 3 seconds, request times out with 504
3. Check logs for timeout events

## Best Practices

### 1. Configuration
- Externalize all settings
- Use environment-specific profiles
- Document all thresholds

### 2. Monitoring
- Monitor all metrics continuously
- Set up alerts for anomalies
- Track trends over time

### 3. Logging
- Log at appropriate levels
- Include context in logs
- Use structured logging

### 4. Testing
- Test each pattern individually
- Test pattern combinations
- Load test before production

### 5. Deployment
- Gradual rollout
- Monitor closely after deployment
- Have rollback plan

### 6. Documentation
- Document all patterns
- Provide examples
- Keep documentation updated

## Performance Characteristics

| Pattern | Overhead | Scalability | Use Case |
|---------|----------|-------------|----------|
| RateLimiter | ~1ms | High | API protection |
| CircuitBreaker | ~0.5ms | High | Failure prevention |
| Retry | Variable | Medium | Transient failures |
| Bulkhead | ~0.5ms | High | Resource isolation |
| TimeLimiter | ~0.5ms | High | Timeout protection |

## Troubleshooting

### High Error Rate
1. Check CircuitBreaker state
2. Review service logs
3. Check downstream service health
4. Verify network connectivity

### Rate Limit Violations
1. Check if limits are appropriate
2. Analyze traffic patterns
3. Consider increasing limits
4. Add more instances

### Performance Issues
1. Monitor metrics
2. Check for thread contention
3. Review timeout configurations
4. Analyze slow requests

## Dependencies

```xml
- resilience4j-spring-boot3
- resilience4j-circuitbreaker
- resilience4j-retry
- resilience4j-bulkhead
- resilience4j-timelimiter
- resilience4j-ratelimiter
- resilience4j-micrometer
- spring-cloud-starter-openfeign
- lombok
```

## Files Structure

```
src/main/java/com/inventory/
├── config/
│   ├── MessageConfiguration.java
│   ├── Resilience4jConfig.java
│   └── WebConfig.java
├── controller/
│   ├── HelloController.java
│   ├── MonitoringController.java
│   └── ServiceBController.java
├── client/
│   └── ServiceBFeignClient.java
├── service/
│   ├── ServiceBCallerService.java
│   └── RateLimiterService.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
├── health/
│   └── Resilience4jHealthIndicator.java
├── interceptor/
│   └── RateLimiterInterceptor.java
└── InventoryServiceApplication.java
```

## Conclusion

This implementation provides a comprehensive, production-grade resilience solution that:
- Prevents cascading failures
- Limits resource consumption
- Handles transient failures
- Provides detailed monitoring
- Follows best practices
- Is easy to maintain and extend

For detailed information on each pattern, refer to:
- `RESILIENCE4J_SETUP.md` - CircuitBreaker, Retry, Bulkhead, TimeLimiter
- `RATELIMITER_SETUP.md` - RateLimiter pattern
