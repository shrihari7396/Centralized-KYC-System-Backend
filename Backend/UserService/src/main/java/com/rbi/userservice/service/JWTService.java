package com.rbi.userservice.service;

import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.util.Date;

public interface JWTService {


    String generateToken(String username, String role);

    String extractUserRole(String token);

    String extractUsername(String token);

    boolean isTokenExpired(String token);

    Date extractExpiration(String token);

    boolean validateToken(String token);

    <T> T extractClaim(String token, java.util.function.Function<io.jsonwebtoken.Claims, T> claimsResolver);

    Claims extractAllClaims(String token);

    SecretKey getSigningKey();
}