package com.inventory.common.util;

import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Interface for token generation following Dependency Inversion Principle.
 * Allows different token implementations without changing dependent code.
 */
public interface TokenProvider {
    String generateToken(Authentication authentication);
    
    String generateTokenFromUsername(String username);
    
    String generateRefreshToken(String username);
    
    String generateTokenWithClaims(String username, Map<String, Object> claims);
    
    String getUsernameFromToken(String token);
    
    Boolean validateToken(String token);
    
    Boolean isTokenExpired(String token);
    
    String refreshToken(String token);
}
