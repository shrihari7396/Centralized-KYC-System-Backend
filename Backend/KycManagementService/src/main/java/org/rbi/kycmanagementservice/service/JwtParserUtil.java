package org.rbi.kycmanagementservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtParserUtil {

    private final SecretKey secretKey;

    public JwtParserUtil(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
            String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String cleanToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return token != null ? token.trim() : null;
    }

    /**
     * Parses and cryptographically validates the token payload against the configured HMAC secret key.
     */
    public Claims parsePayload(String token) {
        String cleaned = cleanToken(token);
        if (cleaned == null || cleaned.isEmpty()) {
            throw new IllegalArgumentException("JWT token is null or empty");
        }
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(cleaned)
                .getPayload();
    }

    /** Extracts user ID from subject or custom userId claim. */
    public String extractUserId(String token) {
        Claims claims = parsePayload(token);
        String userId = claims.get("userId", String.class);
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        return claims.getSubject();
    }

    /** Extracts requested document fields list. */
    @SuppressWarnings("unchecked")
    public List<String> extractRequestedFields(String token) {
        Claims claims = parsePayload(token);
        Object docFields = claims.get("documentFields");
        if (docFields instanceof List<?>) {
            return (List<String>) docFields;
        }
        return new ArrayList<>();
    }

    /** Extracts JWT ID (JTI). */
    public String extractJti(String token) {
        Claims claims = parsePayload(token);
        return claims.getId();
    }

    /** Checks if token is expired. */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parsePayload(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.before(new Date());
        } catch (JwtException e) {
            log.warn("Token expired or invalid: {}", e.getMessage());
            return true;
        }
    }

    /** Extracts remaining seconds before expiration. */
    public long getExpirationRemainingSeconds(String token) {
        try {
            Claims claims = parsePayload(token);
            Date exp = claims.getExpiration();
            if (exp != null) {
                long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000;
                return Math.max(remaining, 0);
            }
        } catch (Exception e) {
            log.warn("Failed to calculate remaining seconds: {}", e.getMessage());
        }
        return 0;
    }
}
