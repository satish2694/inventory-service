# Java 17 Features Used in Inventory Service

This document outlines the Java 17 features implemented in the production-grade microservice.

## 1. Records (Java 16+)

Records provide a concise way to declare immutable data carriers with automatic generation of constructors, accessors, equals(), hashCode(), and toString().

### LoginRequest Record
```java
public record LoginRequest(String username, String password) {
}
```
- Replaces verbose POJO with getters/setters
- Immutable by default
- Automatic accessor methods: `username()` and `password()`

### AuthResponse Record
```java
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String username,
        String message
) {
    // Builder pattern still supported
    public static Builder builder() { ... }
}
```
- Compact representation of API response
- Maintains builder pattern for flexibility
- Automatic equals() and hashCode() implementation

## 2. Pattern Matching for instanceof (Java 16+)

Pattern matching simplifies type checking and casting in a single operation.

### GlobalExceptionHandler
```java
if (ex instanceof CallNotPermittedException e) {
    logger.error("Circuit Breaker is OPEN for request: {}", path, e);
    // Use 'e' directly without explicit casting
    errorResponse = ErrorResponse.builder()
            .details(e.getMessage())
            .build();
} else if (ex instanceof BulkheadFullException e) {
    // Pattern variable 'e' is automatically cast
    logger.warn("Bulkhead is full, request rejected for: {}", path);
    errorResponse = ErrorResponse.builder()
            .details(e.getMessage())
            .build();
}
```

### AuthController
```java
if (authentication != null && authentication.isAuthenticated()) {
    String username = authentication.getName();
    // Direct use without casting
}
```

## 3. Sealed Classes (Java 17)

Sealed classes restrict which classes can extend or implement them, providing better control over inheritance hierarchies.

### ApiResponse Sealed Interface
```java
public sealed interface ApiResponse {
    record SuccessResponse(Object data, String message) implements ApiResponse {}
    record FailureResponse(ErrorResponse error) implements ApiResponse {}
}
```

Benefits:
- Type-safe API responses
- Compiler ensures all permitted implementations are handled
- Better code maintainability
- Prevents unexpected subclassing

## 4. Text Blocks (Java 15+)

While not extensively used in this service, text blocks can be used for multi-line strings like SQL queries or JSON templates.

## 5. Record Accessors

Records automatically generate accessor methods with the same name as the field (no "get" prefix):

```java
// Instead of: loginRequest.getUsername()
// Use: loginRequest.username()
logger.info("Login attempt for user: {}", loginRequest.username());
```

## 6. Sealed Records

Combining sealed classes with records for maximum type safety:

```java
public sealed interface ApiResponse {
    record SuccessResponse(...) implements ApiResponse {}
    record FailureResponse(...) implements ApiResponse {}
}
```

## Java 17 Features NOT Used (Requires Java 21+)

The following Java 21+ features were not used to maintain Java 17 compatibility:

1. **Pattern Matching in switch** - Requires Java 21
   - Would allow: `switch(ex) { case CallNotPermittedException e -> ... }`
   - Currently using if-else with pattern matching instead

2. **Unconditional Patterns in instanceof** - Requires Java 21
   - Would allow: `if (authentication instanceof Authentication auth) { ... }`
   - Currently using null checks with pattern matching

3. **Virtual Threads** - Requires Java 21
   - Would improve concurrency for high-throughput scenarios
   - Can be added in future upgrades

## Performance Benefits

1. **Records**: Reduced memory footprint and faster object creation
2. **Pattern Matching**: Cleaner, more readable code with fewer casting errors
3. **Sealed Classes**: Better compiler optimizations and type safety
4. **Immutability**: Records are immutable by default, reducing bugs

## Migration Path

To upgrade to Java 21 and use additional features:

1. Update pom.xml: `<source>21</source>` and `<target>21</target>`
2. Refactor GlobalExceptionHandler to use switch pattern matching
3. Implement Virtual Threads for async operations
4. Use unconditional patterns in instanceof

## Compatibility

- **Current**: Java 17+
- **Recommended**: Java 17 LTS (Long-Term Support)
- **Future**: Java 21 LTS for additional features

## References

- [Java 17 Features](https://docs.oracle.com/en/java/javase/17/docs/api/)
- [Records](https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.10)
- [Pattern Matching](https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.20.2)
- [Sealed Classes](https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.1.1.2)
