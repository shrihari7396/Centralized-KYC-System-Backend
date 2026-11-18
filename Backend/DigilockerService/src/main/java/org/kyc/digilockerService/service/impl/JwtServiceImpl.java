package org.kyc.digilockerService.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.kyc.digilockerService.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtServiceImpl implements JwtService {

    @Value("${secret.key:${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public List<String> extractRoles(String token) {
        String role = getClaims(token).get("role", String.class);
        return (role != null) ? List.of(role) : List.of();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && !exp.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
