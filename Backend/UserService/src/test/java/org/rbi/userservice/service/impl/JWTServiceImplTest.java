package org.rbi.userservice.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class JWTServiceImplTest {

    private JWTServiceImpl jwtService;
    private final String secret =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JWTServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "expirationInMillis", 3600000L); // 1 hour
    }

    @Test
    void testGenerateToken_AndExtractions() {
        String email = "test@example.com";
        String role = "USER";

        String token = jwtService.generateToken(email, role);

        assertNotNull(token);
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(role, jwtService.extractUserRole(token));
        assertNotNull(jwtService.extractJti(token));
    }

    @Test
    void testIsTokenExpired_False() {
        String token = jwtService.generateToken("test@example.com", "USER");

        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testIsTokenExpired_True() {
        // Set short expiration to test expiry
        ReflectionTestUtils.setField(jwtService, "expirationInMillis", -1000L); // expired 1s ago

        String token = jwtService.generateToken("test@example.com", "USER");

        assertTrue(jwtService.isTokenExpired(token));
    }

    @Test
    void testValidateToken_Valid() {
        String token = jwtService.generateToken("test@example.com", "USER");

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        String token = jwtService.generateToken("test@example.com", "USER");
        String tamperedToken = token + "tamper";

        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    void testExtractExpiration() {
        String token = jwtService.generateToken("test@example.com", "USER");
        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void testExtractAllClaims() {
        String token = jwtService.generateToken("test@example.com", "ADMIN");
        Claims claims = jwtService.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
    }
}
