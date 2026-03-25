package com.inventory.auth;

import com.inventory.common.dto.ApiResponse;
import com.inventory.common.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication endpoints.
 * Follows Single Responsibility Principle - only handles HTTP requests.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login request received for user: {}", loginRequest.username());
        
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (AuthenticationException ex) {
            logger.error("Login failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), "/auth/login"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.debug("Token refresh request received");

        try {
            String refreshToken = extractToken(authHeader);
            AuthResponse response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));

        } catch (AuthenticationException ex) {
            logger.error("Token refresh failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), "/auth/refresh"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        logger.info("Logout request received");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                authService.logout(username);
                SecurityContextHolder.clearContext();
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));

        } catch (Exception ex) {
            logger.error("Logout failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), "/auth/logout"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<AuthResponse>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.debug("Token validation request received");

        try {
            extractToken(authHeader);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                AuthResponse response = AuthResponse.builder()
                        .username(authentication.getName())
                        .message("Token is valid")
                        .build();
                return ResponseEntity.ok(ApiResponse.success(response, "Token is valid"));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Token is invalid or expired", "/auth/validate"));

        } catch (AuthenticationException ex) {
            logger.error("Token validation failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), "/auth/validate"));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException("Invalid authorization header");
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
