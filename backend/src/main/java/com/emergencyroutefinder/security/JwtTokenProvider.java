package com.emergencyroutefinder.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token utility — creates, validates, and parses JWT tokens.
 *
 * Demonstrates: Security, Encapsulation, Exception Handling
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate a JWT token for authenticated user/worker.
     *
     * @param email       the subject (login identifier)
     * @param role        USER or WORKER
     * @param principalId the database ID of the user/worker
     */
    public String generateToken(String email, String role, Long principalId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("principalId", principalId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Extract email (subject) from token. */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extract role claim from token. */
    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).get("role");
    }

    /** Extract principal ID from token. */
    public Long getPrincipalIdFromToken(String token) {
        Object id = parseClaims(token).get("principalId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        return (Long) id;
    }

    /** Validate token signature and expiry. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
