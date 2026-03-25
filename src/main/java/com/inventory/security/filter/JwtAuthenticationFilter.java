package com.inventory.security.filter;

import com.inventory.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                logger.debug("JWT token validated for user: {}", username);

                // Extract authorities from JWT claims
                List<GrantedAuthority> authorities = extractAuthoritiesFromToken(jwt);

                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Set Spring Security authentication for user: {} with authorities: {}", username, authorities);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> extractAuthoritiesFromToken(String token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        try {
            Claims claims = tokenProvider.getAllClaimsFromToken(token);
            
            // Extract authorities from claims
            Object authoritiesObj = claims.get("authorities");
            
            if (authoritiesObj instanceof List<?>) {
                List<?> authList = (List<?>) authoritiesObj;
                for (Object auth : authList) {
                    if (auth instanceof Map<?, ?>) {
                        Map<?, ?> authMap = (Map<?, ?>) auth;
                        Object authority = authMap.get("authority");
                        if (authority != null) {
                            authorities.add(new SimpleGrantedAuthority(authority.toString()));
                            logger.debug("Added authority: {}", authority);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not extract authorities from token: {}", ex.getMessage());
        }
        
        return authorities;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
