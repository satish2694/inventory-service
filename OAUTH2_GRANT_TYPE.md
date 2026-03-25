# OAuth2 Grant Type Implementation

## Grant Type Implemented: **Resource Owner Password Credentials Grant**

Also known as: **Password Grant** or **Direct Authentication Grant**

## Overview

The project implements the **OAuth2 Resource Owner Password Credentials Grant** type, which is a simplified authentication flow suitable for trusted first-party applications.

## Flow Diagram

```
┌─────────────┐                                      ┌──────────────┐
│   Client    │                                      │ Auth Server  │
│ (Frontend)  │                                      │  (Backend)   │
└──────┬──────┘                                      └──────┬───────┘
       │                                                    │
       │  1. POST /auth/login                              │
       │     {username, password}                          │
       ├───────────────────────────────────────────────────>│
       │                                                    │
       │                                    2. Validate credentials
       │                                       (DaoAuthenticationProvider)
       │                                                    │
       │  3. Return tokens                                 │
       │     {accessToken, refreshToken}                   │
       │<───────────────────────────────────────────────────┤
       │                                                    │
       │  4. Store tokens (localStorage/sessionStorage)    │
       │                                                    │
       │  5. Use accessToken in Authorization header       │
       │     GET /inventory-service/callServiceB           │
       │     Authorization: Bearer <accessToken>           │
       ├───────────────────────────────────────────────────>│
       │                                                    │
       │                                    6. Validate JWT token
       │                                       (JwtAuthenticationFilter)
       │                                                    │
       │  7. Return protected resource                     │
       │<───────────────────────────────────────────────────┤
       │                                                    │
```

## Implementation Details

### 1. Login Endpoint
**Endpoint**: `POST /auth/login`

**Request**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response**:
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

### 2. Authentication Process

**AuthService.login()**:
```java
// Step 1: Create authentication token with username and password
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        loginRequest.username(),
        loginRequest.password()
    )
);

// Step 2: Generate JWT tokens
String accessToken = tokenProvider.generateToken(authentication);
String refreshToken = tokenProvider.generateRefreshToken(loginRequest.username());

// Step 3: Return tokens to client
return AuthResponse.builder()
    .accessToken(accessToken)
    .refreshToken(refreshToken)
    .tokenType("Bearer")
    .expiresIn(jwtExpirationMs / 1000)
    .username(loginRequest.username())
    .message("Login successful")
    .build();
```

### 3. Token Validation

**JwtAuthenticationFilter**:
- Intercepts all requests
- Extracts JWT token from `Authorization: Bearer <token>` header
- Validates token signature and expiration
- Sets authentication in SecurityContext

### 4. Protected Resource Access

**Request**:
```bash
curl -X GET http://localhost:8081/inventory-service/callServiceB \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response**: Protected resource data

## Key Components

### 1. AuthService
- Handles authentication logic
- Uses `AuthenticationManager` to validate credentials
- Generates JWT tokens
- Manages token refresh

### 2. JwtTokenProvider
- Generates JWT tokens with HMAC-SHA512
- Validates token signatures
- Extracts claims from tokens
- Handles token expiration

### 3. SecurityConfig
- Configures Spring Security
- Sets up `DaoAuthenticationProvider` for credential validation
- Defines authorization rules
- Configures JWT filter

### 4. JwtAuthenticationFilter
- Intercepts HTTP requests
- Extracts JWT from Authorization header
- Validates token
- Sets authentication in SecurityContext

## Token Details

### Access Token
- **Algorithm**: HMAC-SHA512
- **Expiration**: 24 hours (86400 seconds)
- **Contains**: username, roles, issued time, expiration time
- **Used for**: Accessing protected resources

### Refresh Token
- **Algorithm**: HMAC-SHA512
- **Expiration**: 7 days (604800 seconds)
- **Contains**: username, issued time, expiration time
- **Used for**: Obtaining new access token

## Refresh Token Flow

**Endpoint**: `POST /auth/refresh`

**Request**:
```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Authorization: Bearer <refreshToken>"
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "admin",
  "message": "Token refreshed successfully"
}
```

## Security Features

### 1. Password Encoding
- Uses BCrypt with salt
- Passwords never stored in plain text
- Passwords never transmitted in tokens

### 2. Token Security
- Signed with HMAC-SHA512
- Cannot be forged without secret key
- Includes expiration time
- Includes username and roles

### 3. Stateless Authentication
- No session storage required
- Each request is independent
- Scalable across multiple servers
- No session fixation attacks

### 4. Authorization
- Role-based access control (RBAC)
- Three roles: ADMIN, USER, SERVICE
- Endpoint-level authorization
- Method-level authorization with @PreAuthorize

## Default Users

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER |
| service-user | service123 | SERVICE |

## Comparison with Other Grant Types

| Grant Type | Use Case | Security | Complexity |
|-----------|----------|----------|-----------|
| **Password** (Implemented) | Trusted first-party apps | Medium | Low |
| Authorization Code | Third-party apps | High | High |
| Client Credentials | Service-to-service | Medium | Medium |
| Implicit | Browser-based SPAs | Low | Low |
| Refresh Token | Token renewal | High | Medium |

## Why Password Grant?

### Advantages
✅ Simple to implement
✅ Direct user authentication
✅ No redirect required
✅ Suitable for first-party applications
✅ Good for mobile apps and SPAs

### Disadvantages
❌ Requires sharing credentials with client
❌ Not suitable for third-party apps
❌ Less secure than Authorization Code flow
❌ Client must handle credentials

## Best Practices Implemented

✅ **Stateless Authentication**: No server-side session storage
✅ **JWT Tokens**: Self-contained, verifiable tokens
✅ **Token Expiration**: Short-lived access tokens
✅ **Refresh Tokens**: Separate long-lived tokens for renewal
✅ **Password Encoding**: BCrypt with salt
✅ **HTTPS Ready**: Designed for HTTPS deployment
✅ **Role-Based Access**: Fine-grained authorization
✅ **Audit Logging**: All authentication events logged

## Testing

### Login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Access Protected Resource
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')

curl -X GET http://localhost:8081/inventory-service/callServiceB \
  -H "Authorization: Bearer $TOKEN"
```

### Refresh Token
```bash
REFRESH_TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.refreshToken')

curl -X POST http://localhost:8081/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

### Validate Token
```bash
curl -X POST http://localhost:8081/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

### Logout
```bash
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

## Configuration

### JWT Settings (application.yaml)
```yaml
app:
  jwt:
    secret: thisIsAVeryLongSecretKeyThatIsAtLeast64BytesLongForHS512AlgorithmSecurityComplianceRequirements1234567890
    expiration: 86400000  # 24 hours in milliseconds
    refresh-expiration: 604800000  # 7 days in milliseconds
```

### Security Settings
```yaml
spring:
  security:
    user:
      name: admin
      password: admin123
```

## Summary

The project implements the **OAuth2 Resource Owner Password Credentials Grant** type, which is:

- ✅ Simple and straightforward
- ✅ Suitable for trusted first-party applications
- ✅ Stateless and scalable
- ✅ Secure with JWT tokens and BCrypt passwords
- ✅ Production-ready with proper error handling
- ✅ Supports token refresh and validation
- ✅ Implements role-based authorization

This grant type is ideal for internal microservices, mobile applications, and single-page applications where the client is trusted and can securely handle user credentials.
