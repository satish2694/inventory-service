# Before & After Code Comparison

## 1. Service Call with Resilience Patterns

### BEFORE: ServiceBCallerService (Tightly Coupled)

```java
@Service
public class ServiceBCallerService {
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    private RetryRegistry retryRegistry;
    @Autowired
    private BulkheadRegistry bulkheadRegistry;
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    public String callServiceB(String cookieValue) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("serviceBCircuitBreaker");
        Retry retry = retryRegistry.retry("serviceBRetry");
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("serviceBBulkhead");
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("serviceBRateLimiter");

        Supplier<String> supplier = () -> serviceBFeignClient.displayMessage(cookieValue);
        Supplier<String> rateLimited = RateLimiter.decorateSupplier(rateLimiter, supplier);
        Supplier<String> circuitBreakerDecorated = CircuitBreaker.decorateSupplier(circuitBreaker, rateLimited);
        Supplier<String> retryDecorated = Retry.decorateSupplier(retry, circuitBreakerDecorated);
        Supplier<String> bulkheadDecorated = Bulkhead.decorateSupplier(bulkhead, retryDecorated);

        return bulkheadDecorated.get();
    }
}
```

**Problems**:
- ❌ Tightly coupled to registries
- ❌ Hard to test
- ❌ Hard to add/remove strategies
- ❌ Violates Single Responsibility Principle
- ❌ Violates Dependency Inversion Principle

### AFTER: ServiceBCallerService (Loosely Coupled)

```java
@Service
public class ServiceBCallerService {
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
        Supplier<String> supplier = () -> serviceBFeignClient.displayMessage(cookieValue);
        return resilienceDecorator.execute(supplier, strategies);
    }
}
```

**Benefits**:
- ✅ Loosely coupled to abstractions
- ✅ Easy to test with mocks
- ✅ Easy to add/remove strategies
- ✅ Follows Single Responsibility Principle
- ✅ Follows Dependency Inversion Principle
- ✅ Uses Strategy and Decorator patterns

---

## 2. Authentication Service

### BEFORE: AuthService (Direct Dependency)

```java
@Service
public class AuthService {
    @Autowired
    private JwtTokenProvider tokenProvider;  // Direct dependency

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(...);
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.username());
            return AuthResponse.builder()...build();
        } catch (AuthenticationException ex) {
            throw new RuntimeException("Invalid username or password", ex);  // Generic exception
        }
    }
}
```

**Problems**:
- ❌ Direct dependency on `JwtTokenProvider`
- ❌ Hard to swap implementations
- ❌ Generic exception handling
- ❌ Violates Dependency Inversion Principle

### AFTER: AuthService (Interface Dependency)

```java
@Service
public class AuthService {
    private final TokenProvider tokenProvider;  // Interface dependency

    public AuthService(AuthenticationManager authenticationManager,
                      TokenProvider tokenProvider,
                      @Value("${app.jwt.expiration:86400000}") long jwtExpirationMs) {
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(...);
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.username());
            return AuthResponse.builder()...build();
        } catch (AuthenticationException ex) {
            throw new com.inventory.common.exception.AuthenticationException(
                "Invalid username or password", ex);  // Specific exception
        }
    }
}
```

**Benefits**:
- ✅ Depends on `TokenProvider` interface
- ✅ Easy to swap implementations
- ✅ Specific exception handling
- ✅ Follows Dependency Inversion Principle
- ✅ Constructor injection

---

## 3. API Response Format

### BEFORE: Inconsistent Responses

```java
@GetMapping("/displayMessage")
public ResponseEntity<String> showMessage() {
    return ResponseEntity.ok("Inventory Service controller executed " + message);
}

@GetMapping("/callServiceB")
public ResponseEntity<String> callServiceB(String cookieValue) {
    String response = serviceBCallerService.callServiceB(cookieValue);
    return ResponseEntity.ok("Response from ServiceB: " + response);
}

@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
    AuthResponse response = authService.login(loginRequest);
    return ResponseEntity.ok(response);
}
```

**Problems**:
- ❌ Inconsistent response formats
- ❌ No standardized error responses
- ❌ Hard to parse on client side
- ❌ Violates DRY principle

### AFTER: Consistent Responses

```java
@GetMapping("/displayMessage")
public ResponseEntity<ApiResponse<String>> showMessage() {
    String message = "Inventory Service controller executed " + messageConfiguration.getDisplayMessage();
    return ResponseEntity.ok(ApiResponse.success(message, "Message retrieved"));
}

@GetMapping("/callServiceB")
public ResponseEntity<ApiResponse<String>> callServiceB(String cookieValue) {
    String response = serviceBCallerService.callServiceB(cookieValue);
    return ResponseEntity.ok(ApiResponse.success(response, "ServiceB call successful"));
}

@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest loginRequest) {
    AuthResponse response = authService.login(loginRequest);
    return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
}
```

**Benefits**:
- ✅ Consistent response format
- ✅ Standardized error responses
- ✅ Easy to parse on client side
- ✅ Follows DRY principle
- ✅ Uses Factory pattern

---

## 4. Exception Handling

### BEFORE: Generic Exception Handling

```java
@ExceptionHandler({CallNotPermittedException.class, BulkheadFullException.class, 
                   RequestNotPermitted.class, Exception.class})
public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
    if (ex instanceof CallNotPermittedException e) {
        // Handle circuit breaker
    } else if (ex instanceof BulkheadFullException e) {
        // Handle bulkhead
    } else if (ex instanceof RequestNotPermitted e) {
        // Handle rate limit
    } else {
        // Handle generic
    }
}
```

**Problems**:
- ❌ All exceptions handled in one method
- ❌ Hard to add new exception types
- ❌ Violates Single Responsibility Principle
- ❌ Violates Open/Closed Principle

### AFTER: Specific Exception Handlers

```java
@ExceptionHandler(CallNotPermittedException.class)
public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(
        CallNotPermittedException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Service is temporarily unavailable. Circuit breaker is open.",
                    extractPath(request)));
}

@ExceptionHandler(BulkheadFullException.class)
public ResponseEntity<ApiResponse<Void>> handleBulkheadFull(
        BulkheadFullException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many concurrent requests. Please try again later.",
                    extractPath(request)));
}

@ExceptionHandler(ApplicationException.class)
public ResponseEntity<ApiResponse<Void>> handleApplicationException(
        ApplicationException ex, WebRequest request) {
    return ResponseEntity.status(ex.getStatusCode())
            .body(ApiResponse.error(ex.getStatusCode(), ex.getMessage(), extractPath(request)));
}
```

**Benefits**:
- ✅ Each exception type has its own handler
- ✅ Easy to add new exception types
- ✅ Follows Single Responsibility Principle
- ✅ Follows Open/Closed Principle
- ✅ Uses Template Method pattern

---

## 5. Monitoring Controller

### BEFORE: Tightly Coupled

```java
@RestController
@RequestMapping("/monitoring")
public class MonitoringController {
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    private RetryRegistry retryRegistry;
    @Autowired
    private BulkheadRegistry bulkheadRegistry;
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @GetMapping("/resilience4j/circuitbreaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerDetails() {
        Map<String, Object> details = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", cb.getState().toString());
            // ... more details
            details.put(cb.getName(), cbDetails);
        });
        return ResponseEntity.ok(details);
    }

    @GetMapping("/resilience4j/bulkhead")
    public ResponseEntity<Map<String, Object>> getBulkheadDetails() {
        // Similar implementation
    }

    @GetMapping("/resilience4j/ratelimiter")
    public ResponseEntity<Map<String, Object>> getRateLimiterDetails() {
        // Similar implementation
    }
}
```

**Problems**:
- ❌ Tightly coupled to registries
- ❌ Monitoring logic mixed with HTTP handling
- ❌ Code duplication
- ❌ Hard to test
- ❌ Violates Single Responsibility Principle

### AFTER: Loosely Coupled

```java
@RestController
@RequestMapping("/monitoring")
public class MonitoringController {
    private final ResilienceMonitoringService monitoringService;

    public MonitoringController(ResilienceMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/resilience4j/circuitbreaker")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCircuitBreakerDetails() {
        Map<String, Object> details = monitoringService.getCircuitBreakerDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "CircuitBreaker details retrieved"));
    }

    @GetMapping("/resilience4j/bulkhead")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBulkheadDetails() {
        Map<String, Object> details = monitoringService.getBulkheadDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "Bulkhead details retrieved"));
    }

    @GetMapping("/resilience4j/ratelimiter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimiterDetails() {
        Map<String, Object> details = monitoringService.getRateLimiterDetails();
        return ResponseEntity.ok(ApiResponse.success(details, "RateLimiter details retrieved"));
    }
}
```

**Benefits**:
- ✅ Loosely coupled to service
- ✅ HTTP handling separated from monitoring logic
- ✅ No code duplication
- ✅ Easy to test
- ✅ Follows Single Responsibility Principle
- ✅ Uses Dependency Injection

---

## Summary of Improvements

| Aspect | Before | After |
|--------|--------|-------|
| **Coupling** | Tight | Loose |
| **Testability** | Difficult | Easy |
| **Code Duplication** | High | Minimal |
| **Extensibility** | Limited | Excellent |
| **SOLID Compliance** | Partial | Full |
| **Design Patterns** | Few | Multiple |
| **Maintainability** | Moderate | High |
| **Response Format** | Inconsistent | Consistent |
| **Exception Handling** | Generic | Specific |
| **Dependency Management** | Direct | Interface-based |

---

## Migration Path

1. **Phase 1**: Deploy new classes alongside old ones
2. **Phase 2**: Update controllers to use new services
3. **Phase 3**: Update clients to handle new response format
4. **Phase 4**: Remove old implementations
5. **Phase 5**: Monitor and optimize

---

## Testing Improvements

### BEFORE: Hard to Test

```java
@Test
void testServiceCall() {
    // Hard to mock registries
    // Hard to test different strategy combinations
    // Hard to isolate concerns
}
```

### AFTER: Easy to Test

```java
@Test
void testServiceCall() {
    ResilienceStrategy mockStrategy = mock(ResilienceStrategy.class);
    ResilienceDecorator mockDecorator = mock(ResilienceDecorator.class);
    ServiceBFeignClient mockClient = mock(ServiceBFeignClient.class);
    
    ServiceBCallerService service = new ServiceBCallerService(
        mockClient, mockDecorator, mockStrategy, ...
    );
    
    // Easy to test with mocks
    // Easy to test different combinations
    // Easy to isolate concerns
}
```

---

## Conclusion

The refactoring provides:
- ✅ Better code organization
- ✅ Easier testing
- ✅ Better maintainability
- ✅ Flexible composition
- ✅ SOLID principles compliance
- ✅ DRY implementation
- ✅ Industry-standard design patterns
- ✅ Consistent API responses
- ✅ Specific exception handling
- ✅ Loose coupling
