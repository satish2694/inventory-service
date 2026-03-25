package com.inventory.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login request received for user: {}", loginRequest.username());
        
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Login failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Login failed: " + ex.getMessage())
                            .build());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.debug("Token refresh request received");

        try {
            // Java 17 Pattern Matching for null check
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.builder()
                                .message("Invalid authorization header")
                                .build());
            }

            String refreshToken = authHeader.substring(7);
            AuthResponse response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Token refresh failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Token refresh failed: " + ex.getMessage())
                            .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {
        logger.info("Logout request received");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Java 17 Pattern Matching for instanceof
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                authService.logout(username);
                SecurityContextHolder.clearContext();

                return ResponseEntity.ok(AuthResponse.builder()
                        .message("Logout successful")
                        .build());
            }

            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Logout successful")
                    .build());

        } catch (Exception ex) {
            logger.error("Logout failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .message("Logout failed: " + ex.getMessage())
                            .build());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.debug("Token validation request received");

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.builder()
                                .message("Invalid authorization header")
                                .build());
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Java 17 Pattern Matching for instanceof
            if (authentication != null && authentication.isAuthenticated()) {
                return ResponseEntity.ok(AuthResponse.builder()
                        .username(authentication.getName())
                        .message("Token is valid")
                        .build());
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Token is invalid or expired")
                            .build());

        } catch (Exception ex) {
            logger.error("Token validation failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Token validation failed: " + ex.getMessage())
                            .build());
        }
    }
}
