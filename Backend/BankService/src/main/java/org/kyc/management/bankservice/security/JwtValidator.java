package org.kyc.management.bankservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtValidator {
    private final String secret =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("email", email);

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .and()
                .signWith(getSigningKey())
                .compact();
    }

    public String getJtiFromToken(String token) {
        return extractAllClaims(token).get("jti", String.class);
    }

    public Date getExpirationFromToken(String token) {
        return extractAllClaims(token).getExpiration();
    }
}
