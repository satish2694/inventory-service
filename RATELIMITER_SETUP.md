# Production-Grade RateLimiter Implementation

## Overview
This document describes the production-grade RateLimiter implementation using Resilience4J. The RateLimiter pattern prevents excessive requests to services by limiting the number of requests allowed within a specific time period.

## RateLimiter Patterns Implemented

### 1. Service-Level RateLimiter (serviceBRateLimiter)
- **Purpose**: Limits requests to ServiceB to prevent overload
- **Configuration**:
  - Limit For Period: 50 requests
  - Limit Refresh Period: 1 minute
  - Timeout Duration: 10 seconds
- **Use Case**: Protects downstream service from being overwhelmed

### 2. API-Level RateLimiter (apiRateLimiter)
- **Purpose**: Limits overall API requests to the microservice
- **Configuration**:
  - Limit For Period: 200 requests
  - Limit Refresh Period: 1 minute
  - Timeout Duration: 5 seconds
- **Use Case**: Protects the microservice from being overwhelmed by clients

### 3. Heavy Operation RateLimiter (heavyOperationRateLimiter)
- **Purpose**: Limits resource-intensive operations
- **Configuration**:
  - Limit For Period: 10 requests
  - Limit Refresh Period: 1 minute
  - Timeout Duration: 30 seconds
- **Use Case**: Protects expensive operations like data processing, file uploads, etc.

## Architecture

### Components

#### 1. RateLimiterInterceptor
- Intercepts all incoming HTTP requests
- Applies API-level rate limiting
- Returns 429 (Too Many Requests) when limit is exceeded
- Logs all rate limit violations

#### 2. RateLimiterService
- Provides methods to check rate limit permissions
- Retrieves rate limiter metrics
- Manages rate limiter operations
- Supports custom rate limiting logic

#### 3. Resilience4jConfig
- Configures all RateLimiter instances
- Sets up event listeners for rate limit violations
- Logs rate limiter state changes

#### 4. ServiceBCallerService
- Applies service-level rate limiting to ServiceB calls
- Combines rate limiting with other resilience patterns
- Provides fallback strategy

#### 5. MonitoringController
- Exposes RateLimiter metrics via REST endpoints
- Provides real-time monitoring capabilities
- Supports detailed metrics per rate limiter

## API Endpoints

### Rate Limiter Monitoring Endpoints

```
GET /monitoring/resilience4j/ratelimiter
  - Returns: Metrics for all rate limiters
  - Response: Map of rate limiter names to their metrics

GET /monitoring/resilience4j/ratelimiter/{name}
  - Returns: Detailed metrics for a specific rate limiter
  - Parameters: name - Rate limiter name (e.g., serviceBRateLimiter)
  - Response: Detailed metrics including available permissions

GET /monitoring/resilience4j/status
  - Returns: Overall status of all Resilience4J components including rate limiters

GET /monitoring/resilience4j/all
  - Returns: Comprehensive details of all Resilience4J components
```

### Service Endpoints with Rate Limiting

```
GET /inventory-service/callServiceB?cookie=dark
  - Rate Limiting: Applied at both API level and service level
  - Returns: Response from ServiceB or rate limit error

GET /inventory-service/callServiceBWithFallback?cookie=dark
  - Rate Limiting: Applied with fallback strategy
  - Returns: ServiceB response or fallback message
```

## Rate Limiter Metrics

### Available Metrics

```json
{
  "serviceBRateLimiter": {
    "limitForPeriod": 50,
    "limitRefreshPeriod": "PT1M",
    "timeoutDuration": "PT10S",
    "availablePermissions": 45,
    "numberOfWaitingThreads": 0
  }
}
```

### Metric Descriptions

- **limitForPeriod**: Maximum number of requests allowed per period
- **limitRefreshPeriod**: Time period for rate limit refresh (ISO-8601 format)
- **timeoutDuration**: Maximum time to wait for a permission (ISO-8601 format)
- **availablePermissions**: Current number of available permissions
- **numberOfWaitingThreads**: Number of threads waiting for permission

## Exception Handling

### RequestNotPermitted Exception
- **HTTP Status**: 429 (Too Many Requests)
- **Error Code**: RATE_LIMIT_EXCEEDED
- **Message**: "Rate limit exceeded. Please try again later."
- **Logging**: Warnings logged for rate limit violations

### Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:45.123456",
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please try again later.",
  "details": "Request not permitted",
  "path": "/inventory-service/callServiceB"
}
```

## Configuration

### application.yaml Configuration

```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        registerHealthIndicator: true
        limitRefreshPeriod: 1m
        limitForPeriod: 100
        timeoutDuration: 5s
    instances:
      serviceBRateLimiter:
        baseConfig: default
        limitRefreshPeriod: 1m
        limitForPeriod: 50
        timeoutDuration: 10s
      apiRateLimiter:
        baseConfig: default
        limitRefreshPeriod: 1m
        limitForPeriod: 200
        timeoutDuration: 5s
      heavyOperationRateLimiter:
        baseConfig: default
        limitRefreshPeriod: 1m
        limitForPeriod: 10
        timeoutDuration: 30s
```

## Implementation Details

### Decorator Order in ServiceBCallerService

The order of decorators is important for optimal performance:

```
1. RateLimiter (first) - Checks if request is allowed
2. CircuitBreaker - Prevents cascading failures
3. Retry - Retries failed requests
4. Bulkhead - Limits concurrent calls
5. TimeLimiter (last) - Enforces timeout
```

### Interceptor Configuration

The RateLimiterInterceptor is configured to:
- Apply to: `/inventory-service/**`, `/monitoring/**`
- Exclude: `/actuator/**`
- Return 429 status when limit exceeded
- Log all violations

## Monitoring and Alerting

### Health Indicators
- Rate limiter status available via `/actuator/health`
- Shows available permissions vs. limit for each rate limiter

### Metrics Endpoints
- Detailed metrics available via `/monitoring/resilience4j/ratelimiter`
- Real-time monitoring of rate limiter state

### Logging
- **INFO**: Rate limiter initialization
- **DEBUG**: Permission granted
- **WARN**: Permission denied, rate limit exceeded
- **ERROR**: Errors in rate limiter operations

## Testing Rate Limiters

### Test API-Level Rate Limiting
```bash
# Send 201 requests in quick succession
for i in {1..201}; do
  curl http://localhost:8081/inventory-service/displayMessage
done
# After 200 requests, subsequent requests will return 429
```

### Test Service-Level Rate Limiting
```bash
# Send 51 requests to ServiceB in quick succession
for i in {1..51}; do
  curl http://localhost:8081/inventory-service/callServiceB
done
# After 50 requests, subsequent requests will return 429
```

### Monitor Rate Limiter Status
```bash
curl http://localhost:8081/monitoring/resilience4j/ratelimiter
```

## Best Practices

1. **Appropriate Limits**: Set limits based on actual service capacity
2. **Timeout Configuration**: Balance between fairness and responsiveness
3. **Monitoring**: Continuously monitor rate limiter metrics
4. **Alerting**: Set up alerts for high rate limit violations
5. **Documentation**: Document rate limits for API consumers
6. **Gradual Rollout**: Test rate limiters in staging before production
7. **Client Handling**: Implement exponential backoff in clients
8. **Metrics Analysis**: Analyze metrics to optimize limits

## Integration with Other Patterns

### With CircuitBreaker
- Rate limiter prevents excessive load before circuit breaker opens
- Reduces cascading failures

### With Retry
- Rate limiter allows retries without overwhelming the service
- Prevents retry storms

### With Bulkhead
- Rate limiter complements bulkhead by limiting incoming requests
- Bulkhead limits concurrent execution

### With TimeLimiter
- Rate limiter prevents request queue buildup
- TimeLimiter ensures individual requests don't hang

## Performance Considerations

- **Minimal Overhead**: Rate limiter adds minimal latency (~1ms)
- **Thread-Safe**: Safe for concurrent access
- **Memory Efficient**: Efficient memory usage for tracking permissions
- **Scalable**: Suitable for high-throughput applications

## Future Enhancements

1. **Dynamic Rate Limiting**: Adjust limits based on system load
2. **Per-User Rate Limiting**: Different limits for different users
3. **Distributed Rate Limiting**: Rate limiting across multiple instances
4. **Custom Strategies**: Implement custom rate limiting algorithms
5. **Rate Limit Headers**: Return rate limit info in response headers

## Troubleshooting

### High Rate Limit Violations
- Check if limits are too restrictive
- Analyze traffic patterns
- Consider increasing limits or adding more instances

### Rate Limiter Not Working
- Verify configuration in application.yaml
- Check if interceptor is registered
- Review logs for errors

### Performance Issues
- Monitor rate limiter metrics
- Check for thread contention
- Consider adjusting timeout duration
