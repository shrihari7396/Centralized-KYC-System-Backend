package com.rbi.userservice.service.impl;

import static java.rmi.server.LogStream.log;

import com.rbi.userservice.DTO.*;
import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import com.rbi.userservice.Repository.UserRepository;
import com.rbi.userservice.config.KafkaTopicNameConstants;
import com.rbi.userservice.exception.InValidOtpException;
import com.rbi.userservice.kafkaEvents.CreateOtpEvent;
import com.rbi.userservice.logging.KafkaLogProducer;
import com.rbi.userservice.service.JWTService;
import com.rbi.userservice.service.UserService;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;
    private final KafkaLogProducer kafkaLogProducer;
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom;

    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        return generateAndSendOtp(
                otpRequestDto.getFirstName(),
                otpRequestDto.getLastName(),
                otpRequestDto.getEmail(),
                otpRequestDto.getEmail());
    }

    @Override
    public Boolean sendOtpByPhone(String phone) {
        UserEntity user =
                userRepository
                        .findByPhone(phone)
                        .orElseThrow(
                                () -> {
                                    kafkaLogProducer.logError(
                                            "USER_NOT_FOUND", java.util.Map.of("phone", phone));
                                    return new RuntimeException(
                                            "User not found with phone: " + phone);
                                });
        return generateAndSendOtp(user.getFirstName(), user.getLastName(), user.getEmail(), phone);
    }

    private Boolean generateAndSendOtp(
            String firstName, String lastName, String email, String key) {
        int otp = secureRandom.nextInt(1_000_000);
        String finalotp = String.format("%06d", otp);

        CreateOtpEvent event =
                CreateOtpEvent.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .otp(finalotp)
                        .serviceType("LOGIN")
                        .build();
        log(finalotp);
        System.out.println(finalotp);
        saveOtp(finalotp, key);
        kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_TOPIC, event);

        kafkaLogProducer.logInfo("OTP_SENT", java.util.Map.of("email", email, "key", key));

        return true;
    }

    @Override
    public Boolean registerUser(RegisterDTO user) {
        UserEntity u =
                UserEntity.builder()
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .status(UserStatus.KYC_PENDING)
                        .phone(user.getMobileNumber()) // Fixed field name
                        .build();

        UserEntity saved = userRepository.save(u);
        if (saved != null) {
            kafkaLogProducer.logInfo(
                    "USER_REGISTERED",
                    java.util.Map.of("email", saved.getEmail(), "phone", saved.getPhone()));
        } else {
            kafkaLogProducer.logError(
                    "USER_REGISTRATION_FAILED", java.util.Map.of("email", user.getEmail()));
        }
        return saved != null;
    }

    @Override
    public Boolean verifyOtp(String otp, String key) {
        String StoredOtp = redisTemplate.opsForValue().get(key);
        if (StoredOtp == null) {
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED_EXPIRED", java.util.Map.of("key", key));
            throw new InValidOtpException("Expired or invalid OTP");
        }
        boolean result = StoredOtp.equals(otp);
        if (!result) {
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED_INVALID", java.util.Map.of("key", key));
            throw new InValidOtpException("Invalid OTP");
        }

        kafkaLogProducer.logInfo("OTP_VERIFIED", java.util.Map.of("key", key));
        deleteOtp(key);
        return result;
    }

    @Override
    public void saveOtp(String otp, String key) {
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(10));
    }

    @Override
    public void deleteOtp(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean isExistByEmail(String email) {
        return userRepository.updateLastLogin(email) > 0;
    }

    @Override
    public LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto) {
        LoginResponseDto response = null;

        if (verifyOtp(otpVerificationDto.getOtp(), otpVerificationDto.getEmail())) {
            String token =
                    jwtService.generateToken(
                            otpVerificationDto.getEmail(), otpVerificationDto.getRole().toString());
            response =
                    LoginResponseDto.builder()
                            .role(otpVerificationDto.getRole())
                            .email(otpVerificationDto.getEmail())
                            .firstName(otpVerificationDto.getFirstName())
                            .lastName(otpVerificationDto.getLastName())
                            .token(token)
                            .build();
        }

        if (response == null) {
            kafkaLogProducer.logWarning(
                    "LOGIN_FAILED", java.util.Map.of("email", otpVerificationDto.getEmail()));
            throw new InValidOtpException("Invalid OTP");
        }

        kafkaLogProducer.logInfo(
                "USER_LOGGED_IN",
                java.util.Map.of("email", response.getEmail(), "role", response.getRole()));
        return response;
    }

    @Override
    public Boolean logout(String token) {
        try {
            String jti = jwtService.extractJti(token);
            java.util.Date expiration = jwtService.extractExpiration(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                redisTemplate.opsForValue().set("blacklist:" + jti, "true", Duration.ofMillis(ttl));
                return true;
            }
        } catch (Exception e) {
            kafkaLogProducer.logError(
                    "LOGOUT_FAILED",
                    java.util.Map.of(
                            "token_snippet", token.substring(0, Math.min(token.length(), 10))));
        }
        return false;
    }

    @Override
    public UserStatus getUserStatusByEmail(String email) {
        return userRepository.findUserStatusByEmail(email);
    }

    @Override
    public UserDto getUserDetails(String email) {
        UserEntity user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException("User not found with email: " + email));
        return UserDto.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .build();
    }
}
