package org.rbi.notificationService.security;

import io.jsonwebtoken.Claims;
import java.util.Date;
import javax.crypto.SecretKey;

public interface JWTService {

    String extractUserRole(String token);

    String extractEmail(String token);

    String extractUserId(String token);

    boolean isTokenExpired(String token);

    Date extractExpiration(String token);

    boolean validateToken(String token);

    <T> T extractClaim(
            String token, java.util.function.Function<io.jsonwebtoken.Claims, T> claimsResolver);

    Claims extractAllClaims(String token);

    String extractJti(String token);

    SecretKey getSigningKey();
}
