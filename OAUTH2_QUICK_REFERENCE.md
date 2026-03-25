# OAuth2 Grant Type - Quick Reference

## Grant Type: **Resource Owner Password Credentials Grant**

## Quick Summary

```
User sends username + password → Server validates → Returns JWT tokens
```

## API Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/auth/login` | POST | Get tokens | ❌ No |
| `/auth/refresh` | POST | Refresh access token | ✅ Yes (Refresh Token) |
| `/auth/logout` | POST | Logout user | ✅ Yes (Access Token) |
| `/auth/validate` | POST | Validate token | ✅ Yes (Access Token) |

## Request/Response Examples

### 1. Login
```bash
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

### 2. Access Protected Resource
```bash
GET /inventory-service/callServiceB
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

Response (200 OK):
{
  "data": "Protected resource content"
}
```

### 3. Refresh Token
```bash
POST /auth/refresh
Authorization: Bearer <refreshToken>

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "admin",
  "message": "Token refreshed successfully"
}
```

### 4. Validate Token
```bash
POST /auth/validate
Authorization: Bearer <accessToken>

Response (200 OK):
{
  "username": "admin",
  "message": "Token is valid"
}
```

### 5. Logout
```bash
POST /auth/logout
Authorization: Bearer <accessToken>

Response (200 OK):
{
  "message": "Logout successful"
}
```

## Token Details

### Access Token
- **Lifetime**: 24 hours
- **Algorithm**: HMAC-SHA512
- **Used for**: Accessing protected resources
- **Format**: JWT (JSON Web Token)

### Refresh Token
- **Lifetime**: 7 days
- **Algorithm**: HMAC-SHA512
- **Used for**: Getting new access token
- **Format**: JWT (JSON Web Token)

## Default Users

```
Username: admin
Password: admin123
Roles: ADMIN, USER

Username: user
Password: user123
Roles: USER

Username: service-user
Password: service123
Roles: SERVICE
```

## cURL Examples

### Login and Get Tokens
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Store Token in Variable
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')
```

### Use Token to Access Protected Resource
```bash
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

## Authorization Rules

| Endpoint | Required Role | Public |
|----------|---------------|--------|
| `/auth/**` | None | ✅ Yes |
| `/actuator/**` | None | ✅ Yes |
| `/inventory-service/displayMessage` | None | ✅ Yes |
| `/inventory-service/**` | USER | ❌ No |
| `/monitoring/**` | ADMIN, SERVICE | ❌ No |

## Security Features

✅ **Password Encoding**: BCrypt with salt  
✅ **Token Signing**: HMAC-SHA512  
✅ **Token Expiration**: 24 hours (access), 7 days (refresh)  
✅ **Stateless**: No server-side session storage  
✅ **Role-Based Access**: ADMIN, USER, SERVICE  
✅ **Audit Logging**: All auth events logged  

## Flow Diagram

```
1. User Login
   POST /auth/login {username, password}
   ↓
2. Server Validates Credentials
   DaoAuthenticationProvider checks password
   ↓
3. Generate Tokens
   JwtTokenProvider creates JWT tokens
   ↓
4. Return Tokens to Client
   {accessToken, refreshToken, expiresIn}
   ↓
5. Client Stores Tokens
   localStorage or sessionStorage
   ↓
6. Access Protected Resource
   GET /protected-endpoint
   Authorization: Bearer <accessToken>
   ↓
7. Server Validates Token
   JwtAuthenticationFilter validates JWT
   ↓
8. Return Protected Resource
   {data}
```

## Common Issues & Solutions

### Issue: "Invalid username or password"
**Solution**: Check credentials against default users table

### Issue: "Token is invalid or expired"
**Solution**: Login again to get new token or use refresh token

### Issue: "Full authentication is required"
**Solution**: Add Authorization header with Bearer token

### Issue: "Access Denied"
**Solution**: User doesn't have required role for endpoint

## Key Classes

| Class | Purpose |
|-------|---------|
| `AuthService` | Authentication business logic |
| `AuthController` | REST endpoints |
| `JwtTokenProvider` | JWT token generation/validation |
| `JwtAuthenticationFilter` | Token validation filter |
| `SecurityConfig` | Spring Security configuration |
| `DaoAuthenticationProvider` | Credential validation |

## Configuration

```yaml
app:
  jwt:
    secret: <64+ character secret key>
    expiration: 86400000  # 24 hours
    refresh-expiration: 604800000  # 7 days
```

## Summary

✅ **Grant Type**: Resource Owner Password Credentials  
✅ **Token Type**: JWT (JSON Web Token)  
✅ **Algorithm**: HMAC-SHA512  
✅ **Access Token Lifetime**: 24 hours  
✅ **Refresh Token Lifetime**: 7 days  
✅ **Authentication**: Username + Password  
✅ **Authorization**: Role-based (ADMIN, USER, SERVICE)  
✅ **Stateless**: No server-side sessions  

This is a production-ready OAuth2 implementation suitable for internal microservices and trusted first-party applications.
