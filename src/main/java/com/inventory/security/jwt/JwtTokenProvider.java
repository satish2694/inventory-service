package com.inventory.security.jwt;

import com.inventory.common.util.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationPurposeOnly12345}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    public String generateToken(Authentication authentication) {
        logger.debug("Generating JWT token for user: {}", authentication.getName());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authentication.getAuthorities());
        
        return createToken(claims, authentication.getName(), jwtExpirationMs);
    }

    @Override
    public String generateTokenFromUsername(String username) {
        logger.debug("Generating JWT token for username: {}", username);
        return createToken(new HashMap<>(), username, jwtExpirationMs);
    }

    @Override
    public String generateRefreshToken(String username) {
        logger.debug("Generating refresh token for username: {}", username);
        return createToken(new HashMap<>(), username, refreshTokenExpirationMs);
    }

    @Override
    public String generateTokenWithClaims(String username, Map<String, Object> claims) {
        logger.debug("Generating JWT token with claims for username: {}", username);
        return createToken(claims, username, jwtExpirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception ex) {
            logger.error("Error extracting username from token: {}", ex.getMessage());
            return null;
        }
    }

    private Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception ex) {
            logger.error("Error extracting expiration date from token: {}", ex.getMessage());
            return null;
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception ex) {
            logger.error("Error checking token expiration: {}", ex.getMessage());
            return true;
        }
    }

    @Override
    public Boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            
            if (isTokenExpired(token)) {
                logger.warn("Token is expired");
                return false;
            }
            
            logger.debug("Token validation successful");
            return true;
        } catch (Exception ex) {
            logger.error("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String refreshToken(String token) {
        try {
            if (validateToken(token)) {
                String username = getUsernameFromToken(token);
                return generateTokenFromUsername(username);
            }
        } catch (Exception ex) {
            logger.error("Error refreshing token: {}", ex.getMessage());
        }
        return null;
    }
}
