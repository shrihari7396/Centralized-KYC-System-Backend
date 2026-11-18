package org.rbi.userservice.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.userservice.DTO.*;
import org.rbi.userservice.Model.Role;
import org.rbi.userservice.Model.UserEntity;
import org.rbi.userservice.Model.UserStatus;
import org.rbi.userservice.Repository.UserRepository;
import org.rbi.userservice.config.KafkaTopicNameConstants;
import org.rbi.userservice.exception.InValidOtpException;
import org.rbi.userservice.exception.UserNotFoundException;
import org.rbi.userservice.kafkaEvents.CreateOtpEvent;
import org.rbi.userservice.logging.KafkaLogProducer;
import org.rbi.userservice.service.JWTService;
import org.rbi.userservice.service.UserService;
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

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final Duration OTP_TTL = Duration.ofMinutes(3);
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        return generateAndSendOtp(
                otpRequestDto.getFirstName(),
                otpRequestDto.getLastName(),
                otpRequestDto.getEmail(),
                otpRequestDto.getEmail());
    }

    private Boolean generateAndSendOtp(String firstName, String lastName, String email, String key) {
        String lockoutKey = "otp_locked:" + key;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey))) {
            log.warn("OTP request rejected: account locked for key: {}", maskEmail(key));
            throw new InValidOtpException("Account is temporarily locked due to excessive failed attempts. Please try again later.");
        }

        int otp = secureRandom.nextInt(1_000_000);
        String finalOtp = String.format("%06d", otp);

        CreateOtpEvent event = CreateOtpEvent.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .otp(finalOtp)
                .serviceType("LOGIN")
                .build();

        // Save OTP and reset attempt counter
        saveOtp(finalOtp, key);
        redisTemplate.delete("otp_attempts:" + key);

        kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_TOPIC, event);
        kafkaLogProducer.logInfo("OTP_SENT", Map.of("key", maskEmail(key)));

        return true;
    }

    @Override
    public Boolean registerUser(RegisterDTO user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + user.getEmail() + " is already registered.");
        }

        UserEntity u = UserEntity.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(UserStatus.KYC_PENDING)
                .phone(user.getMobileNumber())
                .roles(Role.USER)
                .build();

        UserEntity saved = userRepository.save(u);
        if (saved != null) {
            kafkaLogProducer.logInfo("USER_REGISTERED", Map.of("userId", saved.getId(), "email", maskEmail(saved.getEmail())));
        } else {
            kafkaLogProducer.logError("USER_REGISTRATION_FAILED", Map.of("email", maskEmail(user.getEmail())));
        }
        return saved != null;
    }

    @Override
    public Boolean verifyOtp(String otp, String key) {
        String lockoutKey = "otp_locked:" + key;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey))) {
            throw new InValidOtpException("Account is temporarily locked due to excessive failed attempts. Please request a new OTP later.");
        }

        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp == null) {
            kafkaLogProducer.logWarning("OTP_VERIFICATION_FAILED_EXPIRED", Map.of("key", maskEmail(key)));
            throw new InValidOtpException("Expired or invalid OTP. Please request a new OTP.");
        }

        String attemptsKey = "otp_attempts:" + key;
        boolean result = storedOtp.equals(otp);

        if (!result) {
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            kafkaLogProducer.logWarning("OTP_VERIFICATION_FAILED_INVALID", Map.of("key", maskEmail(key), "attempt", String.valueOf(attempts)));

            if (attempts != null && attempts >= MAX_OTP_ATTEMPTS) {
                deleteOtp(key);
                redisTemplate.delete(attemptsKey);
                redisTemplate.opsForValue().set(lockoutKey, "locked", LOCKOUT_DURATION);
                log.warn("Account locked for key {} after {} failed attempts", maskEmail(key), attempts);
                throw new InValidOtpException("Account temporarily locked for 15 minutes due to 3 consecutive failed attempts.");
            }

            int remaining = MAX_OTP_ATTEMPTS - (attempts != null ? attempts.intValue() : 1);
            throw new InValidOtpException("Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        kafkaLogProducer.logInfo("OTP_VERIFIED", Map.of("key", maskEmail(key)));
        deleteOtp(key);
        redisTemplate.delete(attemptsKey);
        return true;
    }

    private void saveOtp(String otp, String key) {
        redisTemplate.opsForValue().set(key, otp, OTP_TTL);
    }

    private void deleteOtp(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean isExistByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto) {
        if (!verifyOtp(otpVerificationDto.getOtp(), otpVerificationDto.getEmail())) {
            throw new InValidOtpException("Invalid OTP");
        }

        UserEntity user = userRepository.findByEmail(otpVerificationDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + otpVerificationDto.getEmail()));

        // Update last login timestamp only after successful OTP verification
        userRepository.updateLastLogin(user.getEmail());

        // Secure Role Assignment: Role is derived strictly from DB entity, preventing client-controlled privilege escalation
        String userRole = user.getRoles() != null ? user.getRoles().name() : "USER";
        String token = jwtService.generateToken(user.getEmail(), userRole);

        LoginResponseDto response = LoginResponseDto.builder()
                .role(Role.valueOf(userRole))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .token(token)
                .build();

        kafkaLogProducer.logInfo("USER_LOGGED_IN", Map.of("userId", user.getId(), "role", userRole));
        return response;
    }

    @Override
    public Boolean logout(String token) {
        try {
            String jti = jwtService.extractJti(token);
            java.util.Date expiration = jwtService.extractExpiration(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0 && jti != null) {
                redisTemplate.opsForValue().set("blacklist:" + jti, "true", Duration.ofMillis(ttl));
                return true;
            }
        } catch (Exception e) {
            kafkaLogProducer.logError("LOGOUT_FAILED", Map.of("error", e.getMessage()));
        }
        return false;
    }

    @Override
    public UserStatus getUserStatusByEmail(String email) {
        return userRepository.findUserStatusByEmail(email);
    }

    @Override
    public UserDto getUserDetails(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return buildUserDto(user);
    }

    @Override
    public UserDto getByUserId(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return buildUserDto(user);
    }

    private UserDto buildUserDto(UserEntity user) {
        return UserDto.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(List.of(user.getRoles() != null ? user.getRoles().name() : "USER"))
                .status(user.getStatus())
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
