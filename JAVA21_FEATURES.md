# Java 21 Features Used in Inventory Service

This document outlines the Java 21 features implemented in the production-grade microservice.

## Installation

### Install JDK 21 via Homebrew
```bash
brew install openjdk@21
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version  # Verify installation
```

## Java 21 Features Implemented

### 1. Records (Java 16+, Stable in Java 21)

Records provide a concise way to declare immutable data carriers with automatic generation of constructors, accessors, equals(), hashCode(), and toString().

#### LoginRequest Record
```java
public record LoginRequest(String username, String password) {
}
```
- Replaces verbose POJO with getters/setters
- Immutable by default
- Automatic accessor methods: `username()` and `password()`

#### AuthResponse Record
```java
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String username,
        String message
) {
    public static Builder builder() { ... }
}
```

### 2. Pattern Matching for instanceof (Java 16+, Stable in Java 21)

Pattern matching simplifies type checking and casting in a single operation.

#### GlobalExceptionHandler
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
}
```

#### AuthController
```java
if (authentication != null && authentication.isAuthenticated()) {
    String username = authentication.getName();
    // Direct use without casting
}
```

### 3. Sealed Classes (Java 17, Stable in Java 21)

Sealed classes restrict which classes can extend or implement them, providing better control over inheritance hierarchies.

#### ApiResponse Sealed Interface
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

### 4. Record Accessors (Java 16+)

Records automatically generate accessor methods with the same name as the field (no "get" prefix):

```java
// Instead of: loginRequest.getUsername()
// Use: loginRequest.username()
logger.info("Login attempt for user: {}", loginRequest.username());
```

### 5. Sealed Records

Combining sealed classes with records for maximum type safety:

```java
public sealed interface ApiResponse {
    record SuccessResponse(...) implements ApiResponse {}
    record FailureResponse(...) implements ApiResponse {}
}
```

### 6. Text Blocks (Java 15+, Stable in Java 21)

While not extensively used in this service, text blocks can be used for multi-line strings like SQL queries or JSON templates.

## Java 21 Exclusive Features (Not Used - Preview)

The following Java 21 features are available but not used to maintain stability:

1. **Pattern Matching in switch** - Preview feature in Java 21
   - Would allow: `switch(ex) { case CallNotPermittedException e -> ... }`
   - Currently using if-else with pattern matching instead
   - Becomes stable in Java 24

2. **Record Patterns** - Preview feature in Java 21
   - Would allow destructuring records in patterns
   - Becomes stable in Java 24

3. **Virtual Threads** - Preview feature in Java 21
   - Lightweight threads for high-throughput scenarios
   - Can be added in future upgrades
   - Becomes stable in Java 24

## Java 21 Compiler Configuration

The pom.xml is configured for Java 21:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
        <release>21</release>
    </configuration>
</plugin>
```

## Performance Benefits

1. **Records**: Reduced memory footprint and faster object creation
2. **Pattern Matching**: Cleaner, more readable code with fewer casting errors
3. **Sealed Classes**: Better compiler optimizations and type safety
4. **Immutability**: Records are immutable by default, reducing bugs
5. **Virtual Threads**: Better scalability for concurrent operations (when used)

## Migration Path to Java 24+

To upgrade to Java 24+ and use additional preview features:

1. Update pom.xml: `<source>24</source>` and `<target>24</target>`
2. Add compiler argument: `<arg>--enable-preview</arg>`
3. Refactor GlobalExceptionHandler to use switch pattern matching
4. Implement Virtual Threads for async operations
5. Use record patterns for destructuring

## Compatibility

- **Current**: Java 21 LTS (Long-Term Support)
- **Recommended**: Java 21 LTS for production
- **Future**: Java 24+ for additional preview features

## Build and Run

```bash
# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run

# Or with Java 21 explicitly
/opt/homebrew/opt/openjdk@21/bin/java -jar target/inventory-service-0.0.1-SNAPSHOT.jar
```

## References

- [Java 21 Features](https://docs.oracle.com/en/java/javase/21/docs/api/)
- [Records](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10)
- [Pattern Matching](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2)
- [Sealed Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.2)
- [Virtual Threads](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html)
