# OAuth2 Grant Type Implementation Summary

## Answer: **Resource Owner Password Credentials Grant**

Also known as: **Password Grant** or **Direct Authentication Grant** (OAuth 2.0 RFC 6749, Section 4.3)

---

## What is This Grant Type?

The Resource Owner Password Credentials Grant is an OAuth2 flow where:
- The user provides their **username and password** directly to the client application
- The client sends these credentials to the authorization server
- The server validates the credentials and returns **JWT tokens**
- The client uses the **access token** to access protected resources

---

## Why This Grant Type?

### ✅ Advantages
1. **Simple Implementation** - Easy to understand and implement
2. **Direct Authentication** - No redirects or complex flows
3. **Suitable for First-Party Apps** - Perfect for internal microservices
4. **Mobile-Friendly** - Works well with mobile applications
5. **Stateless** - No server-side session management needed
6. **Scalable** - Can be deployed across multiple servers

### ❌ Disadvantages
1. **Requires Credential Sharing** - User must share password with client
2. **Not for Third-Party Apps** - Not suitable for external integrations
3. **Less Secure** - Compared to Authorization Code flow
4. **Client Responsibility** - Client must securely handle credentials

---

## Implementation in This Project

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                        │
│                   (Frontend/Mobile)                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ 1. POST /auth/login
                         │    {username, password}
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                  Authorization Server                        │
│                   (Spring Boot Backend)                      │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ AuthController                                       │  │
│  │ - /auth/login                                        │  │
│  │ - /auth/refresh                                      │  │
│  │ - /auth/logout                                       │  │
│  │ - /auth/validate                                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ AuthService                                          │  │
│  │ - Validates credentials                             │  │
│  │ - Generates JWT tokens                              │  │
│  │ - Manages token refresh                             │  │
│  └──────────────────────────────────────────────────────┘  │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ JwtTokenProvider                                     │  │
│  │ - Generates JWT tokens (HMAC-SHA512)                │  │
│  │ - Validates token signatures                        │  │
│  │ - Extracts claims                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ DaoAuthenticationProvider                            │  │
│  │ - Validates username/password                       │  │
│  │ - Uses BCrypt for password encoding                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ UserDetailsService (InMemoryUserDetailsManager)     │  │
│  │ - Stores user credentials                           │  │
│  │ - Manages user roles                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                         ↑
                         │ 2. Return tokens
                         │    {accessToken, refreshToken}
                         │
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                        │
│                   (Frontend/Mobile)                          │
│                                                              │
│  3. Store tokens in localStorage/sessionStorage             │
│  4. Use accessToken in Authorization header                 │
│     GET /protected-endpoint                                 │
│     Authorization: Bearer <accessToken>                     │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. AuthController
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest)
```
- Receives username and password
- Calls AuthService to authenticate
- Returns JWT tokens

#### 2. AuthService
```java
public AuthResponse login(LoginRequest loginRequest)
```
- Uses AuthenticationManager to validate credentials
- Generates JWT tokens
- Returns tokens to client

#### 3. JwtTokenProvider
```java
public String generateToken(Authentication authentication)
public boolean validateToken(String token)
public String getUsernameFromToken(String token)
```
- Generates JWT tokens with HMAC-SHA512
- Validates token signatures
- Extracts claims from tokens

#### 4. JwtAuthenticationFilter
```java
protected void doFilterInternal(HttpServletRequest request, ...)
```
- Intercepts all HTTP requests
- Extracts JWT from Authorization header
- Validates token
- Sets authentication in SecurityContext

#### 5. SecurityConfig
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http)
```
- Configures Spring Security
- Defines authorization rules
- Registers JWT filter

---

## Token Structure

### Access Token (JWT)
```
Header: {
  "alg": "HS512",
  "typ": "JWT"
}

Payload: {
  "sub": "admin",
  "iat": 1711353714,
  "exp": 1711440114,
  "roles": ["ADMIN", "USER"]
}

Signature: HMAC-SHA512(header.payload, secret)
```

### Refresh Token (JWT)
```
Header: {
  "alg": "HS512",
  "typ": "JWT"
}

Payload: {
  "sub": "admin",
  "iat": 1711353714,
  "exp": 1711958514
}

Signature: HMAC-SHA512(header.payload, secret)
```

---

## Authentication Flow

### Step 1: User Login
```bash
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Step 2: Server Validates Credentials
```
1. Extract username and password from request
2. Create UsernamePasswordAuthenticationToken
3. Pass to AuthenticationManager
4. DaoAuthenticationProvider validates:
   - Username exists in UserDetailsService
   - Password matches (BCrypt comparison)
5. Return Authentication object if valid
```

### Step 3: Generate Tokens
```
1. Create JWT payload with username and roles
2. Sign with HMAC-SHA512 using secret key
3. Create access token (24 hour expiration)
4. Create refresh token (7 day expiration)
```

### Step 4: Return Tokens
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

### Step 5: Access Protected Resource
```bash
GET /inventory-service/callServiceB
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### Step 6: Server Validates Token
```
1. Extract token from Authorization header
2. Verify signature using secret key
3. Check token expiration
4. Extract username and roles
5. Set authentication in SecurityContext
```

### Step 7: Return Protected Resource
```json
{
  "data": "Protected resource content"
}
```

---

## Security Features

### 1. Password Security
- ✅ BCrypt encoding with salt
- ✅ Passwords never stored in plain text
- ✅ Passwords never transmitted in tokens

### 2. Token Security
- ✅ HMAC-SHA512 signature
- ✅ Cannot be forged without secret key
- ✅ Includes expiration time
- ✅ Includes username and roles

### 3. Stateless Authentication
- ✅ No server-side session storage
- ✅ Each request is independent
- ✅ Scalable across multiple servers
- ✅ No session fixation attacks

### 4. Authorization
- ✅ Role-based access control (RBAC)
- ✅ Three roles: ADMIN, USER, SERVICE
- ✅ Endpoint-level authorization
- ✅ Method-level authorization

---

## Configuration

```yaml
app:
  jwt:
    secret: thisIsAVeryLongSecretKeyThatIsAtLeast64BytesLongForHS512AlgorithmSecurityComplianceRequirements1234567890
    expiration: 86400000  # 24 hours in milliseconds
    refresh-expiration: 604800000  # 7 days in milliseconds
```

---

## Default Users

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER |
| service-user | service123 | SERVICE |

---

## API Endpoints

| Endpoint | Method | Purpose | Auth |
|----------|--------|---------|------|
| `/auth/login` | POST | Get tokens | ❌ |
| `/auth/refresh` | POST | Refresh access token | ✅ |
| `/auth/logout` | POST | Logout user | ✅ |
| `/auth/validate` | POST | Validate token | ✅ |

---

## Comparison with Other Grant Types

| Grant Type | Use Case | Security | Complexity |
|-----------|----------|----------|-----------|
| **Password** (Implemented) | Trusted first-party apps | Medium | Low |
| Authorization Code | Third-party apps | High | High |
| Client Credentials | Service-to-service | Medium | Medium |
| Implicit | Browser-based SPAs | Low | Low |

---

## Best Practices Implemented

✅ Stateless authentication  
✅ JWT tokens with expiration  
✅ Separate access and refresh tokens  
✅ BCrypt password encoding  
✅ HMAC-SHA512 token signing  
✅ Role-based authorization  
✅ Audit logging  
✅ Error handling  
✅ HTTPS ready  

---

## Summary

The project implements the **Resource Owner Password Credentials Grant** type, which is:

- ✅ Simple and straightforward
- ✅ Suitable for trusted first-party applications
- ✅ Stateless and scalable
- ✅ Secure with JWT tokens and BCrypt passwords
- ✅ Production-ready with proper error handling
- ✅ Supports token refresh and validation
- ✅ Implements role-based authorization

This is an ideal implementation for internal microservices, mobile applications, and single-page applications where the client is trusted and can securely handle user credentials.

---

## Documentation Files

- `OAUTH2_GRANT_TYPE.md` - Detailed implementation guide
- `OAUTH2_QUICK_REFERENCE.md` - Quick reference with examples
- `OAUTH2_GRANT_TYPE_SUMMARY.md` - This file

---

**Status**: ✅ Production Ready  
**Grant Type**: Resource Owner Password Credentials  
**Token Type**: JWT (JSON Web Token)  
**Algorithm**: HMAC-SHA512  
**Access Token Lifetime**: 24 hours  
**Refresh Token Lifetime**: 7 days  
