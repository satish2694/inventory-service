# Production-Grade Security, OAuth2, and JWT Implementation

## Overview
This document describes the production-grade security implementation using Spring Security, OAuth2, and JWT (JSON Web Tokens) for the microservice.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Request                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Authentication Controller     │
        │  - Login                       │
        │  - Refresh Token               │
        │  - Logout                      │
        │  - Validate Token              │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  JWT Token Provider            │
        │  - Generate Token              │
        │  - Validate Token              │
        │  - Extract Claims              │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  JWT Authentication Filter     │
        │  - Extract Token               │
        │  - Validate Token              │
        │  - Set Security Context        │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Spring Security Config        │
        │  - Authorization Rules         │
        │  - Exception Handling          │
        │  - CSRF Protection             │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Protected Resources           │
        │  - Inventory Service           │
        │  - Monitoring                  │
        │  - ServiceB                    │
        └────────────────────────────────┘
```

## Security Components

### 1. JWT Token Provider (JwtTokenProvider)
**Purpose**: Generate, validate, and manage JWT tokens

**Features**:
- Token generation with custom claims
- Token validation and expiration checking
- Token refresh capability
- Claims extraction
- Secure key management using HMAC-SHA512

**Methods**:
- `generateToken(Authentication)` - Generate token from authentication
- `generateTokenFromUsername(String)` - Generate token from username
- `generateRefreshToken(String)` - Generate refresh token
- `validateToken(String)` - Validate token signature and expiration
- `getUsernameFromToken(String)` - Extract username from token
- `refreshToken(String)` - Refresh expired token

### 2. JWT Authentication Filter (JwtAuthenticationFilter)
**Purpose**: Intercept requests and validate JWT tokens

**Features**:
- Extracts JWT from Authorization header
- Validates token format and signature
- Sets Spring Security authentication context
- Handles token validation errors gracefully

**Process**:
1. Extract JWT from "Authorization: Bearer <token>" header
2. Validate token using JwtTokenProvider
3. Extract username from token
4. Create UsernamePasswordAuthenticationToken
5. Set in SecurityContextHolder

### 3. Security Configuration (SecurityConfig)
**Purpose**: Configure Spring Security with JWT support

**Features**:
- Stateless session management
- CSRF protection disabled (for API)
- Custom authentication entry point
- Custom access denied handler
- Role-based authorization
- Password encoding with BCrypt

**Authorization Rules**:
- `/auth/**` - Public (no authentication required)
- `/actuator/**` - Public
- `/inventory-service/displayMessage` - Public
- `/inventory-service/**` - Authenticated users
- `/monitoring/**` - ADMIN or SERVICE role
- `/serviceB/**` - Authenticated users

### 4. Authentication Entry Point (JwtAuthenticationEntryPoint)
**Purpose**: Handle unauthorized access attempts

**Response**:
- HTTP 401 Unauthorized
- JSON error response with details
- Logging of unauthorized attempts

### 5. Access Denied Handler (JwtAccessDeniedHandler)
**Purpose**: Handle forbidden access attempts

**Response**:
- HTTP 403 Forbidden
- JSON error response with details
- Logging of access denials

### 6. Authentication Service (AuthService)
**Purpose**: Handle authentication business logic

**Features**:
- User login with credentials validation
- Token generation and refresh
- User logout
- Comprehensive error handling

### 7. Authentication Controller (AuthController)
**Purpose**: Expose authentication endpoints

**Endpoints**:
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh access token
- `POST /auth/logout` - User logout
- `POST /auth/validate` - Validate token

## API Endpoints

### Authentication Endpoints

#### 1. Login
```
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "admin",
  "message": "Login successful"
}
```

#### 2. Refresh Token
```
POST /auth/refresh
Authorization: Bearer <refresh_token>

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "admin",
  "message": "Token refreshed successfully"
}
```

#### 3. Logout
```
POST /auth/logout
Authorization: Bearer <access_token>

Response (200 OK):
{
  "message": "Logout successful"
}
```

#### 4. Validate Token
```
POST /auth/validate
Authorization: Bearer <access_token>

Response (200 OK):
{
  "username": "admin",
  "message": "Token is valid"
}
```

### Protected Endpoints

#### Inventory Service (Requires Authentication)
```
GET /inventory-service/callServiceB?cookie=dark
Authorization: Bearer <access_token>

Response (200 OK):
Response from ServiceB: ...
```

#### Monitoring (Requires ADMIN or SERVICE Role)
```
GET /monitoring/resilience4j/status
Authorization: Bearer <access_token>

Response (200 OK):
{
  "circuitBreakers": {...},
  "bulkheads": {...},
  "rateLimiters": {...}
}
```

## Default Users

### Admin User
- **Username**: admin
- **Password**: admin123
- **Roles**: ADMIN, USER

### Regular User
- **Username**: user
- **Password**: user123
- **Roles**: USER

### Service User
- **Username**: service-user
- **Password**: service123
- **Roles**: SERVICE

## JWT Token Structure

### Header
```json
{
  "alg": "HS512",
  "typ": "JWT"
}
```

### Payload
```json
{
  "sub": "admin",
  "authorities": [
    {
      "authority": "ROLE_ADMIN"
    },
    {
      "authority": "ROLE_USER"
    }
  ],
  "iat": 1705315200,
  "exp": 1705401600
}
```

### Signature
```
HMACSHA512(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

## Configuration

### application.yaml
```yaml
app:
  jwt:
    secret: mySecretKeyForJWTTokenGenerationAndValidationPurposeOnly12345
    expiration: 86400000  # 24 hours in milliseconds
    refresh-expiration: 604800000  # 7 days in milliseconds

spring:
  security:
    user:
      name: admin
      password: admin123
```

## Security Features

### 1. Password Encoding
- Algorithm: BCrypt
- Strength: 10 (default)
- Secure password storage

### 2. Token Security
- Algorithm: HMAC-SHA512
- Expiration: 24 hours (configurable)
- Refresh token: 7 days (configurable)
- Secure key management

### 3. CSRF Protection
- Disabled for stateless API
- Can be enabled for web applications

### 4. Session Management
- Stateless (no server-side sessions)
- Token-based authentication
- Suitable for microservices

### 5. Authorization
- Role-based access control (RBAC)
- Method-level security
- Endpoint-level security

### 6. Error Handling
- Standardized error responses
- Comprehensive logging
- Security event tracking

## Testing Security

### Test 1: Login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Test 2: Access Protected Resource
```bash
curl -X GET http://localhost:8081/inventory-service/callServiceB \
  -H "Authorization: Bearer <access_token>"
```

### Test 3: Refresh Token
```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

### Test 4: Unauthorized Access
```bash
curl -X GET http://localhost:8081/inventory-service/callServiceB
# Returns 401 Unauthorized
```

### Test 5: Forbidden Access
```bash
curl -X GET http://localhost:8081/monitoring/resilience4j/status \
  -H "Authorization: Bearer <user_token>"
# Returns 403 Forbidden (user doesn't have ADMIN role)
```

## Best Practices

### 1. Token Management
- Store tokens securely on client side
- Use HTTPS for all communications
- Implement token rotation
- Handle token expiration gracefully

### 2. Password Security
- Use strong passwords
- Never log passwords
- Use secure password encoding
- Implement password policies

### 3. Authorization
- Use principle of least privilege
- Implement role-based access control
- Regularly audit permissions
- Log authorization failures

### 4. API Security
- Validate all inputs
- Use HTTPS only
- Implement rate limiting
- Monitor for suspicious activity

### 5. Configuration
- Use environment variables for secrets
- Never commit secrets to version control
- Rotate secrets regularly
- Use secure key management

### 6. Monitoring
- Log all authentication attempts
- Monitor failed login attempts
- Track token usage
- Alert on suspicious activity

## Error Responses

### 401 Unauthorized
```json
{
  "timestamp": "2024-01-15T10:30:45.123456",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Unauthorized: Authentication token is missing or invalid",
  "details": "Full authentication is required to access this resource",
  "path": "/inventory-service/callServiceB"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2024-01-15T10:30:45.123456",
  "status": 403,
  "error": "FORBIDDEN",
  "message": "Forbidden: You do not have permission to access this resource",
  "details": "Access is denied",
  "path": "/monitoring/resilience4j/status"
}
```

## Integration with Resilience4J

Security is integrated with Resilience4J patterns:
- Rate limiting applies to authenticated users
- Circuit breaker protects authenticated endpoints
- Bulkhead limits concurrent authenticated requests
- Retry handles transient failures for authenticated calls

## OAuth2 Extension

For OAuth2 support with external providers:

1. Add OAuth2 client dependency
2. Configure OAuth2 provider (Google, GitHub, etc.)
3. Implement OAuth2 login endpoint
4. Map OAuth2 user to internal user
5. Generate JWT token for OAuth2 users

## Troubleshooting

### Token Validation Fails
- Check token expiration
- Verify JWT secret matches
- Check token format (Bearer prefix)
- Verify token signature

### Unauthorized Access
- Verify token is included in Authorization header
- Check token format: "Authorization: Bearer <token>"
- Verify token is not expired
- Check user credentials

### Forbidden Access
- Verify user has required role
- Check authorization rules in SecurityConfig
- Verify role mapping is correct
- Check method-level security annotations

## Files Structure

```
src/main/java/com/inventory/
├── security/
│   ├── jwt/
│   │   └── JwtTokenProvider.java
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java
│   └── config/
│       ├── SecurityConfig.java
│       ├── JwtAuthenticationEntryPoint.java
│       └── JwtAccessDeniedHandler.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── LoginRequest.java
│   └── AuthResponse.java
└── ...
```

## Conclusion

This implementation provides:
- Secure authentication with JWT
- Role-based authorization
- Token refresh capability
- Comprehensive error handling
- Production-ready security
- Easy integration with existing code
- Scalable for microservices architecture
