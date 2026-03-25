# Quick Reference Guide

## Build & Run

```bash
# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

## Authentication

### Login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Response
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "admin",
  "message": "Login successful"
}
```

### Refresh Token
```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

### Logout
```bash
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer <access_token>"
```

### Validate Token
```bash
curl -X POST http://localhost:8081/auth/validate \
  -H "Authorization: Bearer <access_token>"
```

## Protected Endpoints

### Call Service
```bash
curl -X GET http://localhost:8081/inventory-service/callServiceB \
  -H "Authorization: Bearer <access_token>"
```

### Call Service with Fallback
```bash
curl -X GET http://localhost:8081/inventory-service/callServiceBWithFallback \
  -H "Authorization: Bearer <access_token>"
```

## Monitoring

### Overall Status
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/status \
  -H "Authorization: Bearer <admin_token>"
```

### CircuitBreaker Details
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/circuitbreaker \
  -H "Authorization: Bearer <admin_token>"
```

### Bulkhead Details
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/bulkhead \
  -H "Authorization: Bearer <admin_token>"
```

### RateLimiter Details
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/ratelimiter \
  -H "Authorization: Bearer <admin_token>"
```

### All Components
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/all \
  -H "Authorization: Bearer <admin_token>"
```

## Audit Logs

### All Logs
```bash
curl -X GET http://localhost:8081/security/audit/logs \
  -H "Authorization: Bearer <admin_token>"
```

### User Logs
```bash
curl -X GET http://localhost:8081/security/audit/logs/user/admin \
  -H "Authorization: Bearer <admin_token>"
```

### Event Logs
```bash
curl -X GET http://localhost:8081/security/audit/logs/event/AUTHENTICATION \
  -H "Authorization: Bearer <admin_token>"
```

### Clear Logs
```bash
curl -X DELETE http://localhost:8081/security/audit/logs/clear \
  -H "Authorization: Bearer <admin_token>"
```

## Health & Metrics

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

### Metrics
```bash
curl http://localhost:8081/actuator/metrics
```

## Default Users

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER |
| service-user | service123 | SERVICE |

## Configuration

### JWT Settings (application.yaml)
```yaml
app:
  jwt:
    secret: mySecretKeyForJWTTokenGenerationAndValidationPurposeOnly12345
    expiration: 86400000  # 24 hours
    refresh-expiration: 604800000  # 7 days
```

### Resilience4J Settings (application.yaml)
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

## Common Errors

### 401 Unauthorized
- Missing or invalid token
- Token expired
- Invalid token format

**Solution**: Login again and get new token

### 403 Forbidden
- User doesn't have required role
- Insufficient permissions

**Solution**: Use user with appropriate role

### 429 Too Many Requests
- Rate limit exceeded
- Bulkhead full

**Solution**: Wait and retry

### 503 Service Unavailable
- Circuit breaker open
- Retry exhausted

**Solution**: Wait for circuit breaker to recover

### 504 Gateway Timeout
- Request exceeded timeout
- Service is slow

**Solution**: Check service health and retry

## Testing Workflow

### 1. Login
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')
```

### 2. Test Protected Endpoint
```bash
curl -X GET http://localhost:8081/inventory-service/callServiceB \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Check Monitoring
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/status \
  -H "Authorization: Bearer $TOKEN"
```

### 4. View Audit Logs
```bash
curl -X GET http://localhost:8081/security/audit/logs \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Refresh Token
```bash
NEW_TOKEN=$(curl -s -X POST http://localhost:8081/auth/refresh \
  -H "Authorization: Bearer $TOKEN" | jq -r '.accessToken')
```

### 6. Logout
```bash
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

## Performance Testing

### Rate Limiting Test
```bash
# Send 201 requests
for i in {1..201}; do
  curl -s http://localhost:8081/inventory-service/displayMessage
done
# After 200, requests return 429
```

### Concurrent Requests Test
```bash
# Send 10 concurrent requests
for i in {1..10}; do
  curl -s -X GET http://localhost:8081/inventory-service/callServiceB \
    -H "Authorization: Bearer $TOKEN" &
done
wait
```

## Troubleshooting

### Check Logs
```bash
# View application logs
tail -f logs/application.log

# Filter for errors
grep ERROR logs/application.log

# Filter for security events
grep SECURITY logs/application.log
```

### Check Health
```bash
curl http://localhost:8081/actuator/health | jq
```

### Check Metrics
```bash
curl http://localhost:8081/actuator/metrics | jq
```

### Check Audit Logs
```bash
curl -X GET http://localhost:8081/security/audit/logs \
  -H "Authorization: Bearer <admin_token>" | jq
```

## Development Tips

### Enable Debug Logging
Add to application.yaml:
```yaml
logging:
  level:
    com.inventory: DEBUG
    org.springframework.security: DEBUG
```

### Disable Rate Limiting
Temporarily set high limit in application.yaml:
```yaml
resilience4j:
  ratelimiter:
    instances:
      apiRateLimiter:
        limitForPeriod: 10000
```

### Disable Circuit Breaker
Temporarily set high threshold in application.yaml:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      serviceBCircuitBreaker:
        failureRateThreshold: 100
```

## Useful Commands

### Maven
```bash
# Clean build
mvn clean

# Build without tests
mvn install -DskipTests

# Run tests
mvn test

# Run specific test
mvn test -Dtest=TestClassName
```

### Git
```bash
# Clone repository
git clone <repo-url>

# Create branch
git checkout -b feature/new-feature

# Commit changes
git commit -m "Add new feature"

# Push changes
git push origin feature/new-feature
```

### Docker
```bash
# Build image
docker build -t inventory-service:latest .

# Run container
docker run -p 8081:8081 inventory-service:latest

# View logs
docker logs <container-id>
```

## Documentation Files

- `README.md` - Quick start
- `SECURITY_OAUTH2_JWT_SETUP.md` - Security details
- `RESILIENCE4J_SETUP.md` - Resilience details
- `RATELIMITER_SETUP.md` - RateLimiter details
- `COMPLETE_IMPLEMENTATION_GUIDE.md` - Complete guide
- `COMPONENT_INVENTORY.md` - Component list

## Support

For issues:
1. Check logs
2. Review audit logs
3. Check metrics
4. Review documentation
5. Check source code comments

---

**Last Updated**: January 2024
**Version**: 0.0.1-SNAPSHOT
