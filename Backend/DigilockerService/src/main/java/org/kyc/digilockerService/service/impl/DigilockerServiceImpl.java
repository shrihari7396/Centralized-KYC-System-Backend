package org.kyc.digilockerService.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.client.UserClient;
import org.kyc.digilockerService.config.KafkaConstants;
import org.kyc.digilockerService.dto.OtpGenerateRequest;
import org.kyc.digilockerService.dto.UserDto;
import org.kyc.digilockerService.event.Address;
import org.kyc.digilockerService.event.DigiLockerDataFetchedEvent;
import org.kyc.digilockerService.exceptions.InValidOtpException;
import org.kyc.digilockerService.kafka.CreateOtpEvent;
import org.kyc.digilockerService.logging.KafkaLogProducer;
import org.kyc.digilockerService.service.DigilockerService;
import org.kyc.digilockerService.util.RandomDate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DigilockerServiceImpl implements DigilockerService {

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(3);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final KafkaLogProducer kafkaLogProducer;
    private final SecureRandom secureRandom;
    private final UserClient userClient;

    @Override
    public Boolean generateAndSendOtp(OtpGenerateRequest otpGenerateRequest) {
        String key = otpGenerateRequest.getEmail();
        String lockKey = "otp_locked:" + key;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("OTP request rejected: account/email {} is locked due to excessive failed attempts", maskEmail(key));
            throw new InValidOtpException("Account temporarily locked due to excessive failed OTP attempts. Please try again after 15 minutes.");
        }

        int otp = secureRandom.nextInt(1_000_000);
        String finalotp = String.format("%06d", otp);

        CreateOtpEvent event =
                CreateOtpEvent.builder()
                        .firstName(otpGenerateRequest.getFirstname())
                        .lastName(otpGenerateRequest.getLastname())
                        .email(otpGenerateRequest.getEmail())
                        .otp(finalotp)
                        .serviceType("DIGILOCKER")
                        .build();

        saveOtp(key, finalotp);
        // Note: Raw OTP output to stdout purged for security compliance
        log.info("Securely generated DigiLocker OTP for email: {}", maskEmail(key));
        kafkaTemplate.send(KafkaConstants.OTP_CREATED_TOPIC, event.getEmail(), event);

        kafkaLogProducer.logInfo(
                "OTP_SENT", Map.of("email", maskEmail(otpGenerateRequest.getEmail())));

        return true;
    }

    @Override
    public Boolean verifyOtp(String otp, String key) {
        String lockKey = "otp_locked:" + key;
        String attemptKey = "otp_attempts:" + key;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("OTP verification rejected: account {} is temporarily locked", maskEmail(key));
            throw new InValidOtpException("Account temporarily locked due to excessive failed attempts. Please try again later.");
        }

        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp == null) {
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED_EXPIRED", Map.of("key", maskEmail(key)));
            throw new InValidOtpException("Expired or invalid OTP");
        }

        boolean result = storedOtp.equals(otp);
        if (!result) {
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            if (attempts != null && attempts >= MAX_OTP_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCKOUT_DURATION);
                redisTemplate.delete(attemptKey);
                deleteOtp(key);
                log.warn("Account {} locked for 15 minutes after {} failed attempts", maskEmail(key), attempts);
                throw new InValidOtpException("Too many invalid attempts. Account locked for 15 minutes.");
            } else {
                redisTemplate.expire(attemptKey, LOCKOUT_DURATION);
            }
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED_INVALID", Map.of("key", maskEmail(key), "attempt", String.valueOf(attempts)));
            throw new InValidOtpException("Invalid OTP. Attempt " + attempts + " of " + MAX_OTP_ATTEMPTS);
        }

        // On success, reset attempts and remove OTP
        redisTemplate.delete(attemptKey);
        deleteOtp(key);
        kafkaLogProducer.logInfo("OTP_VERIFIED", Map.of("key", maskEmail(key)));
        return true;
    }

    @Override
    public void processVerification(String userId, String email) {
        log.info("Processing verification for user: {}, email: {}", userId, maskEmail(email));
        kafkaLogProducer.logInfo(
                "DIGILOCKER_VERIFICATION_STARTED", Map.of("userId", userId != null ? userId : ""));
        Address address =
                Address.builder()
                        .state("Maharashtra")
                        .city("Pune")
                        .street("FC Road")
                        .country("India")
                        .zipCode("411043")
                        .build();

        // Fetch mobile number from User Service
        String mobileNumber = null;
        try {
            UserDto userDto = userClient.getUserDetails(email);
            if (userDto != null) {
                mobileNumber = userDto.getPhone();
                log.info("Fetched user mobile number for user: {}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user details for email {}: {}", maskEmail(email), e.getMessage());
        }

        // Generate verified KYC payload for DigiLocker simulation
        DigiLockerDataFetchedEvent event =
                DigiLockerDataFetchedEvent.builder()
                        .userId(userId)
                        .mobileNumber(mobileNumber)
                        .email(email)
                        .aadharNumber(UUID.randomUUID().toString())
                        .panNumber(UUID.randomUUID().toString())
                        .firstName("John")
                        .lastName("Doe")
                        .middleName("Quincy")
                        .dob(RandomDate.generateRandomDOB())
                        .address(address)
                        .additionalDetails(
                                new HashMap<>(Map.of("requestId", UUID.randomUUID().toString())))
                        .build();

        kafkaTemplate.send(KafkaConstants.DIGILOCKER_DATA_FETCHED_TOPIC, email, event);

        kafkaLogProducer.logInfo(
                "DIGILOCKER_DATA_FETCHED",
                Map.of("userId", userId != null ? userId : ""));
    }

    private void deleteOtp(String key) {
        redisTemplate.delete(key);
    }

    private void saveOtp(String key, String otp) {
        redisTemplate.opsForValue().set(key, otp, OTP_VALIDITY);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "masked-user";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
