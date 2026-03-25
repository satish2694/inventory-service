package com.inventory.auth;

import com.inventory.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.username());

            logger.info("User {} logged in successfully", loginRequest.username());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs / 1000)
                    .username(loginRequest.username())
                    .message("Login successful")
                    .build();

        } catch (AuthenticationException ex) {
            logger.error("Authentication failed for user {}: {}", loginRequest.username(), ex.getMessage());
            throw new RuntimeException("Invalid username or password", ex);
        }
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        logger.debug("Refreshing access token");

        try {
            if (tokenProvider.validateToken(refreshToken)) {
                String username = tokenProvider.getUsernameFromToken(refreshToken);
                String newAccessToken = tokenProvider.generateTokenFromUsername(username);

                logger.info("Access token refreshed for user: {}", username);

                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtExpirationMs / 1000)
                        .username(username)
                        .message("Token refreshed successfully")
                        .build();
            }
        } catch (Exception ex) {
            logger.error("Error refreshing token: {}", ex.getMessage());
        }

        throw new RuntimeException("Invalid refresh token");
    }

    public void logout(String username) {
        logger.info("User {} logged out", username);
    }
}
