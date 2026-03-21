package com.rbi.userservice.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import com.rbi.userservice.Repository.UserRepository;
import com.rbi.userservice.config.KafkaTopicNameConstants;
import com.rbi.userservice.exception.InValidOtpException;
import com.rbi.userservice.kafkaEvents.CreateOtpEvent;
import com.rbi.userservice.logging.KafkaLogProducer;
import com.rbi.userservice.service.JWTService;
import java.security.SecureRandom;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;
    @Mock private KafkaLogProducer kafkaLogProducer;
    @Mock private JWTService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private SecureRandom secureRandom;

    @InjectMocks private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testSendOtp_Success() {
        OtpRequestDto req = new OtpRequestDto();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john@example.com");

        when(secureRandom.nextInt(1_000_000)).thenReturn(123456);

        Boolean result = userService.sendOtp(req);

        assertTrue(result);
        verify(valueOperations).set(eq("john@example.com"), eq("123456"), any());
        verify(kafkaTemplate)
                .send(eq(KafkaTopicNameConstants.OTP_CREATED_TOPIC), any(CreateOtpEvent.class));
    }

    @Test
    void testSendOtpByPhone_Success() {
        UserEntity user =
                UserEntity.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .phone("1234567890")
                        .build();

        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(user));
        when(secureRandom.nextInt(1_000_000)).thenReturn(654321);

        Boolean result = userService.sendOtpByPhone("1234567890");

        assertTrue(result);
        verify(valueOperations).set(eq("1234567890"), eq("654321"), any());
    }

    @Test
    void testSendOtpByPhone_UserNotFound() {
        when(userRepository.findByPhone("0000000000")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.sendOtpByPhone("0000000000"));
    }

    @Test
    void testRegisterUser_Success() {
        RegisterDTO dto = new RegisterDTO();
        dto.setFirstName("Jane");
        dto.setLastName("Doe");
        dto.setEmail("jane@example.com");
        dto.setMobileNumber("9876543210");

        UserEntity savedUser =
                UserEntity.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .email("jane@example.com")
                        .phone("9876543210")
                        .status(UserStatus.KYC_PENDING)
                        .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        Boolean result = userService.registerUser(dto);

        assertTrue(result);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void testVerifyOtp_Success() {
        when(valueOperations.get("john@example.com")).thenReturn("123456");

        Boolean result = userService.verifyOtp("123456", "john@example.com");

        assertTrue(result);
        verify(redisTemplate).delete("john@example.com");
    }

    @Test
    void testVerifyOtp_Expired() {
        when(valueOperations.get("expired@example.com")).thenReturn(null);

        assertThrows(
                InValidOtpException.class,
                () -> userService.verifyOtp("123456", "expired@example.com"));
    }

    @Test
    void testVerifyOtp_Invalid() {
        when(valueOperations.get("john@example.com")).thenReturn("123456");

        assertThrows(
                InValidOtpException.class,
                () -> userService.verifyOtp("wrong", "john@example.com"));
    }

    @Test
    void testLogout_Success() throws Exception {
        String token = "valid.token.here";
        java.util.Date exp = new java.util.Date(System.currentTimeMillis() + 100000); // in future

        when(jwtService.extractJti(token)).thenReturn("jti-123");
        when(jwtService.extractExpiration(token)).thenReturn(exp);

        Boolean result = userService.logout(token);

        assertTrue(result);
        verify(valueOperations).set(eq("blacklist:jti-123"), eq("true"), any());
    }

    @Test
    void testLogout_Exception() {
        String token = "invalid.token";
        when(jwtService.extractJti(token)).thenThrow(new RuntimeException("parse error"));

        Boolean result = userService.logout(token);

        assertFalse(result);
    }
}
