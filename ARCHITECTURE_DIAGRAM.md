# Architecture & Component Relationships

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           HTTP Clients                                       │
│                    (Web, Mobile, Desktop Apps)                              │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   Spring Security      │
                    │   (JWT Filter)         │
                    └────────────┬────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────────────────┐
│                         REST Controllers                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ AuthController   │  │ HelloController  │  │ MonitoringCtrl   │          │
│  │                  │  │                  │  │                  │          │
│  │ • login()        │  │ • displayMsg()   │  │ • getStatus()    │          │
│  │ • refresh()      │  │ • callServiceB() │  │ • getMetrics()   │          │
│  │ • logout()       │  │ • withFallback() │  │ • getAll()       │          │
│  │ • validate()     │  │                  │  │                  │          │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘          │
└───────────┼──────────────────────┼──────────────────────┼───────────────────┘
            │                      │                      │
            ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Business Services                                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ AuthService      │  │ServiceBCaller    │  │ MonitoringServ   │          │
│  │                  │  │                  │  │                  │          │
│  │ • login()        │  │ • callServiceB() │  │ • getCircuitBr() │          │
│  │ • refresh()      │  │ • withFallback() │  │ • getBulkhead()  │          │
│  │ • logout()       │  │                  │  │ • getRateLim()   │          │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘          │
└───────────┼──────────────────────┼──────────────────────┼───────────────────┘
            │                      │                      │
            ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Abstraction Layer                                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ TokenProvider    │  │ResilienceDecorator│  │ MonitoringServ   │          │
│  │ (Interface)      │  │ (Decorator)      │  │ (Interface)      │          │
│  │                  │  │                  │  │                  │          │
│  │ • generateToken()│  │ • decorate()     │  │ • getDetails()   │          │
│  │ • validateToken()│  │ • execute()      │  │ • getAllDetails()│          │
│  │ • refreshToken() │  │                  │  │                  │          │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘          │
└───────────┼──────────────────────┼──────────────────────┼───────────────────┘
            │                      │                      │
            ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Strategy Pattern Layer                                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ JwtTokenProvider │  │ResilienceStrategy│  │ MonitoringServ   │          │
│  │ (Implementation) │  │ (Interface)      │  │ (Implementation) │          │
│  │                  │  │                  │  │                  │          │
│  │ • generateToken()│  │ • decorate()     │  │ • getDetails()   │          │
│  │ • validateToken()│  │ • getName()      │  │ • getAllDetails()│          │
│  │ • refreshToken() │  │                  │  │                  │          │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘          │
└───────────┼──────────────────────┼──────────────────────┼───────────────────┘
            │                      │                      │
            │          ┌───────────┼───────────┐          │
            │          │           │           │          │
            │          ▼           ▼           ▼          │
            │    ┌──────────────────────────────────┐     │
            │    │  Concrete Strategies             │     │
            │    │  ┌────────────────────────────┐  │     │
            │    │  │ CircuitBreakerStrategy     │  │     │
            │    │  │ RetryStrategy              │  │     │
            │    │  │ BulkheadStrategy           │  │     │
            │    │  │ RateLimiterStrategyImpl     │  │     │
            │    │  └────────────────────────────┘  │     │
            │    └──────────────────────────────────┘     │
            │                                              │
            ▼                                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    External Dependencies                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ Spring Security  │  │ Resilience4j     │  │ Feign Client     │          │
│  │ (Auth Manager)   │  │ (Registries)     │  │ (ServiceB)       │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Component Interaction Diagram

### Authentication Flow

```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /auth/login
     │ {username, password}
     ▼
┌──────────────────┐
│ AuthController   │
└────┬─────────────┘
     │
     ▼
┌──────────────────┐
│ AuthService      │
└────┬─────────────┘
     │
     ├─► AuthenticationManager.authenticate()
     │
     ├─► TokenProvider.generateToken()
     │   └─► JwtTokenProvider.generateToken()
     │
     ├─► TokenProvider.generateRefreshToken()
     │   └─► JwtTokenProvider.generateRefreshToken()
     │
     ▼
┌──────────────────┐
│ AuthResponse     │
│ {accessToken,    │
│  refreshToken}   │
└────┬─────────────┘
     │
     ▼
┌─────────┐
│ Client  │
└─────────┘
```

### Service Call with Resilience Flow

```
┌─────────┐
│ Client  │
└────┬────┘
     │ GET /inventory-service/callServiceB
     ▼
┌──────────────────┐
│ HelloController  │
└────┬─────────────┘
     │
     ▼
┌──────────────────────┐
│ServiceBCallerService │
└────┬─────────────────┘
     │
     ├─► Create Supplier
     │   └─► serviceBFeignClient.displayMessage()
     │
     ├─► ResilienceDecorator.execute()
     │   │
     │   ├─► RateLimiterStrategy.decorate()
     │   │   └─► RateLimiter.decorateSupplier()
     │   │
     │   ├─► CircuitBreakerStrategy.decorate()
     │   │   └─► CircuitBreaker.decorateSupplier()
     │   │
     │   ├─► RetryStrategy.decorate()
     │   │   └─► Retry.decorateSupplier()
     │   │
     │   └─► BulkheadStrategy.decorate()
     │       └─► Bulkhead.decorateSupplier()
     │
     ├─► Execute decorated supplier
     │   └─► Call ServiceB
     │
     ▼
┌──────────────────┐
│ ApiResponse      │
│ {status, data}   │
└────┬─────────────┘
     │
     ▼
┌─────────┐
│ Client  │
└─────────┘
```

### Monitoring Flow

```
┌─────────┐
│ Client  │
└────┬────┘
     │ GET /monitoring/resilience4j/all
     ▼
┌──────────────────────┐
│ MonitoringController │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────────┐
│ResilienceMonitoringServ  │
└────┬─────────────────────┘
     │
     ├─► getCircuitBreakerDetails()
     │   └─► CircuitBreakerRegistry.getAllCircuitBreakers()
     │
     ├─► getBulkheadDetails()
     │   └─► BulkheadRegistry.getAllBulkheads()
     │
     ├─► getRateLimiterDetails()
     │   └─► RateLimiterRegistry.getAllRateLimiters()
     │
     └─► getRetryDetails()
         └─► RetryRegistry.getAllRetries()
     │
     ▼
┌──────────────────┐
│ ApiResponse      │
│ {status, data}   │
└────┬─────────────┘
     │
     ▼
┌─────────┐
│ Client  │
└─────────┘
```

---

## Dependency Injection Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Container                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Singleton Beans                                          │  │
│  │                                                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ AuthService                                        │  │  │
│  │ │ ├─ authenticationManager (injected)               │  │  │
│  │ │ ├─ tokenProvider: TokenProvider (interface)       │  │  │
│  │ │ │  └─ JwtTokenProvider (implementation)           │  │  │
│  │ │ └─ jwtExpirationMs (injected)                     │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ ServiceBCallerService                             │  │  │
│  │ ├─ serviceBFeignClient (injected)                   │  │  │
│  │ ├─ resilienceDecorator (injected)                   │  │  │
│  │ ├─ circuitBreakerStrategy (injected)                │  │  │
│  │ ├─ retryStrategy (injected)                         │  │  │
│  │ ├─ bulkheadStrategy (injected)                      │  │  │
│  │ └─ rateLimiterStrategy (injected)                   │  │  │
│  │ └─ strategies: List<ResilienceStrategy>             │  │  │
│  │    └─ [RateLimiter, CircuitBreaker, Retry, Bulkhead]│  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ ResilienceMonitoringServiceImpl                    │  │  │
│  │ ├─ circuitBreakerRegistry (injected)                │  │  │
│  │ ├─ retryRegistry (injected)                         │  │  │
│  │ ├─ bulkheadRegistry (injected)                      │  │  │
│  │ └─ rateLimiterRegistry (injected)                   │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ ResilienceDecorator                               │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ Strategy Implementations                           │  │  │
│  │ ├─ CircuitBreakerStrategy                           │  │  │
│  │ ├─ RetryStrategy                                    │  │  │
│  │ ├─ BulkheadStrategy                                 │  │  │
│  │ └─ RateLimiterStrategyImpl                           │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ Controllers                                        │  │  │
│  │ ├─ AuthController                                   │  │  │
│  │ ├─ HelloController                                  │  │  │
│  │ └─ MonitoringController                             │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Exception Handling Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Exception Hierarchy                       │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Throwable                                              │ │
│  │ └─ Exception                                           │ │
│  │    └─ ApplicationException (Custom Base)              │ │
│  │       ├─ AuthenticationException                      │ │
│  │       │  └─ HTTP 401 Unauthorized                     │ │
│  │       └─ ServiceCallException                         │ │
│  │          └─ HTTP 503 Service Unavailable              │ │
│  │                                                        │ │
│  │ Resilience4j Exceptions                               │ │
│  │ ├─ CallNotPermittedException                          │ │
│  │ │  └─ HTTP 503 Service Unavailable                    │ │
│  │ ├─ BulkheadFullException                              │ │
│  │ │  └─ HTTP 429 Too Many Requests                      │ │
│  │ └─ RequestNotPermitted                                │ │
│  │    └─ HTTP 429 Too Many Requests                      │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  Exception Handling Flow:                                   │
│                                                              │
│  Exception Thrown                                           │
│       │                                                     │
│       ▼                                                     │
│  GlobalExceptionHandler                                     │
│       │                                                     │
│       ├─► @ExceptionHandler(CallNotPermittedException)     │
│       ├─► @ExceptionHandler(BulkheadFullException)         │
│       ├─► @ExceptionHandler(RequestNotPermitted)           │
│       ├─► @ExceptionHandler(ApplicationException)          │
│       └─► @ExceptionHandler(Exception)                     │
│       │                                                     │
│       ▼                                                     │
│  ApiResponse<Void> Error Response                          │
│  {status, message, timestamp, path}                        │
│       │                                                     │
│       ▼                                                     │
│  HTTP Response                                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

### Request Processing

```
HTTP Request
    │
    ▼
Spring Security Filter Chain
    │
    ├─► JwtAuthenticationFilter
    │   ├─► Extract token from header
    │   ├─► TokenProvider.validateToken()
    │   └─► Set SecurityContext
    │
    ▼
DispatcherServlet
    │
    ▼
Controller
    │
    ├─► Validate input
    ├─► Call Service
    │
    ▼
Service
    │
    ├─► Business logic
    ├─► Call external services
    ├─► Handle exceptions
    │
    ▼
Response
    │
    ├─► Wrap in ApiResponse
    ├─► Set status code
    ├─► Add timestamp
    │
    ▼
HTTP Response
```

---

## Resilience Pattern Composition

```
Original Request
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ ResilienceDecorator.decorate()                          │
│                                                         │
│ Supplier<T> decorated = supplier;                      │
│                                                         │
│ for (ResilienceStrategy strategy : strategies) {       │
│     decorated = strategy.decorate(decorated);          │
│ }                                                       │
│                                                         │
│ return decorated;                                      │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 1: RateLimiter                                    │
│ Limits requests per time period                        │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 2: CircuitBreaker                                 │
│ Prevents cascading failures                            │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 3: Retry                                          │
│ Retries on transient failures                          │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 4: Bulkhead                                       │
│ Isolates resources                                     │
└─────────────────────────────────────────────────────────┘
    │
    ▼
Actual Service Call
    │
    ▼
Response/Exception
```

---

## Class Relationships

```
┌─────────────────────────────────────────────────────────────┐
│ Interfaces (Abstractions)                                   │
│                                                             │
│ ┌──────────────────┐  ┌──────────────────┐                │
│ │ TokenProvider    │  │ResilienceStrategy│                │
│ │ <<interface>>    │  │ <<interface>>    │                │
│ └──────────────────┘  └──────────────────┘                │
│          ▲                      ▲                          │
│          │ implements           │ implements              │
│          │                      │                          │
│ ┌────────┴──────────┐  ┌────────┴──────────────────────┐  │
│ │JwtTokenProvider   │  │ CircuitBreakerStrategy       │  │
│ │ <<component>>     │  │ RetryStrategy                │  │
│ └───────────────────┘  │ BulkheadStrategy             │  │
│                        │ RateLimiterStrategyImpl       │  │
│                        │ <<component>>                │  │
│                        └────────────────────────────────┘  │
│                                                             │
│ ┌──────────────────┐  ┌──────────────────┐                │
│ │ResilienceMonitor │  │ResilienceDecorator              │
│ │Service           │  │ <<component>>                  │
│ │<<interface>>     │  │                                │
│ └──────────────────┘  │ Uses:                          │
│          ▲            │ - List<ResilienceStrategy>     │
│          │ implements │ - Composes strategies          │
│          │            └────────────────────────────────┘  │
│ ┌────────┴──────────┐                                     │
│ │ResilienceMonitor  │                                     │
│ │ServiceImpl         │                                     │
│ │ <<component>>     │                                     │
│ └───────────────────┘                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Services (Business Logic)                                   │
│                                                             │
│ ┌──────────────────┐  ┌──────────────────┐                │
│ │ AuthService      │  │ServiceBCaller    │                │
│ │ <<service>>      │  │Service           │                │
│ │                  │  │ <<service>>      │                │
│ │ Uses:            │  │                  │                │
│ │ - TokenProvider  │  │ Uses:            │                │
│ │ - AuthManager    │  │ - ResilienceDecor│                │
│ │                  │  │ - ResilienceStrat│                │
│ └──────────────────┘  │ - ServiceBClient │                │
│                        └──────────────────┘                │
│                                                             │
│ ┌──────────────────┐                                       │
│ │ResilienceMonitor │                                       │
│ │ServiceImpl        │                                       │
│ │ <<service>>      │                                       │
│ │                  │                                       │
│ │ Uses:            │                                       │
│ │ - Registries     │                                       │
│ └──────────────────┘                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Controllers (HTTP Handlers)                                 │
│                                                             │
│ ┌──────────────────┐  ┌──────────────────┐                │
│ │ AuthController   │  │HelloController   │                │
│ │ <<controller>>   │  │ <<controller>>   │                │
│ │                  │  │                  │                │
│ │ Uses:            │  │ Uses:            │                │
│ │ - AuthService    │  │ - ServiceBCaller │                │
│ │ - ApiResponse    │  │ - ApiResponse    │                │
│ └──────────────────┘  │ - MessageConfig  │                │
│                        └──────────────────┘                │
│                                                             │
│ ┌──────────────────┐                                       │
│ │MonitoringCtrl    │                                       │
│ │ <<controller>>   │                                       │
│ │                  │                                       │
│ │ Uses:            │                                       │
│ │ - MonitoringServ │                                       │
│ │ - ApiResponse    │                                       │
│ └──────────────────┘                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary

This architecture provides:
- ✅ Clear separation of concerns
- ✅ Loose coupling through interfaces
- ✅ High cohesion within components
- ✅ Easy to test and maintain
- ✅ Easy to extend and modify
- ✅ Follows SOLID principles
- ✅ Uses industry-standard design patterns
- ✅ Production-ready implementation
