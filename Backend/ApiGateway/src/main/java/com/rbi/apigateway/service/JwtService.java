package com.rbi.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

    private final String jwtSecret;

    public JwtService(
            @Value("${jwt.secret:${jwt.token:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}}")
            String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    private SecretKey getSecret() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecret())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration() != null && !claims.getExpiration().before(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserRoles(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null) {
            role = claims.get("roles", String.class);
        }
        return role != null ? role : "USER";
    }

    public String getUserId(Claims claims) {
        String sub = claims.getSubject();
        String userId = claims.get("userId", String.class);
        return userId != null ? userId : sub;
    }

    public String getBankId(Claims claims) {
        return claims.get("bankId", String.class);
    }

    public String getJti(Claims claims) {
        String jti = claims.getId();
        if (jti == null) {
            jti = claims.get("jti", String.class);
        }
        return jti;
    }
}
