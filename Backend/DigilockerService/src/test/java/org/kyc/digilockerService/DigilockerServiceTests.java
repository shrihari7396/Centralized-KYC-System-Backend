package org.kyc.digilockerService;

import java.security.SecureRandom;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.digilockerService.client.UserClient;
import org.kyc.digilockerService.dto.OtpGenerateRequest;
import org.kyc.digilockerService.exceptions.InValidOtpException;
import org.kyc.digilockerService.kafka.CreateOtpEvent;
import org.kyc.digilockerService.logging.KafkaLogProducer;
import org.kyc.digilockerService.service.impl.DigilockerServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigilockerServiceTests {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private KafkaLogProducer kafkaLogProducer;
    @Mock private UserClient userClient;

    private DigilockerServiceImpl digilockerService;
    private final SecureRandom secureRandom = new SecureRandom();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        digilockerService = new DigilockerServiceImpl(
                kafkaTemplate, redisTemplate, kafkaLogProducer, secureRandom, userClient);
    }

    @Test
    @DisplayName("generateAndSendOtp sends OTP and stores in Redis with safe validity")
    void testGenerateOtpSuccess() {
        when(redisTemplate.hasKey("otp_locked:user@example.com")).thenReturn(false);

        OtpGenerateRequest req = new OtpGenerateRequest();
        req.setEmail("user@example.com");
        req.setFirstname("Jane");
        req.setLastname("Doe");

        Boolean result = digilockerService.generateAndSendOtp(req);
        assertTrue(result);

        verify(valueOperations, times(1)).set(eq("user@example.com"), anyString(), eq(Duration.ofMinutes(3)));
        verify(kafkaTemplate, times(1)).send(anyString(), eq("user@example.com"), any(CreateOtpEvent.class));
    }

    @Test
    @DisplayName("verifyOtp succeeds and clears attempts on matching OTP")
    void testVerifyOtpSuccess() {
        when(redisTemplate.hasKey("otp_locked:user@example.com")).thenReturn(false);
        when(valueOperations.get("user@example.com")).thenReturn("123456");

        Boolean result = digilockerService.verifyOtp("123456", "user@example.com");
        assertTrue(result);

        verify(redisTemplate, times(1)).delete("otp_attempts:user@example.com");
        verify(redisTemplate, times(1)).delete("user@example.com");
    }

    @Test
    @DisplayName("verifyOtp locks account after 3 failed attempts")
    void testVerifyOtpLockout() {
        when(redisTemplate.hasKey("otp_locked:user@example.com")).thenReturn(false);
        when(valueOperations.get("user@example.com")).thenReturn("123456");
        when(valueOperations.increment("otp_attempts:user@example.com")).thenReturn(3L);

        assertThrows(InValidOtpException.class, () ->
                digilockerService.verifyOtp("999999", "user@example.com"));

        verify(valueOperations, times(1)).set(eq("otp_locked:user@example.com"), eq("LOCKED"), eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("generateAndSendOtp rejects locked accounts")
    void testLockedAccountOtpRequestRejected() {
        when(redisTemplate.hasKey("otp_locked:user@example.com")).thenReturn(true);

        OtpGenerateRequest req = new OtpGenerateRequest();
        req.setEmail("user@example.com");

        assertThrows(InValidOtpException.class, () ->
                digilockerService.generateAndSendOtp(req));
    }
}
