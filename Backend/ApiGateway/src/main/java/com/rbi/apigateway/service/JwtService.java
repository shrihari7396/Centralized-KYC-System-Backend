package com.rbi.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.token}")
    private String jwtSecret;

    private SecretKey getSecret() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUserRoles(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(getSecret())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            return claims.get("role", String.class);

        } catch (JwtException e) {
            throw new JwtException("Invalid token");
        }
    }
}
