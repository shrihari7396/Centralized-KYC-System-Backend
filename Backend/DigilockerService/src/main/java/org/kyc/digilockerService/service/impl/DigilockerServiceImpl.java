package org.kyc.digilockerService.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.config.KafkaTopicNameConstants;
import org.kyc.digilockerService.dto.OtpGenerateRequest;
import org.kyc.digilockerService.event.DigiLockerDataFetchedEvent;
import org.kyc.digilockerService.exceptions.InValidOtpException;
import org.kyc.digilockerService.kafka.CreateOtpEvent;
import org.kyc.digilockerService.logging.KafkaLogProducer;
import org.kyc.digilockerService.service.DigilockerService;
import org.kyc.digilockerService.util.RandomDate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DigilockerServiceImpl implements DigilockerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaLogProducer kafkaLogProducer;
    private final SecureRandom secureRandom;

    public Boolean generateAndSendOtp(OtpGenerateRequest otpGenerateRequest) {
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

        saveOtp(finalotp, event.getEmail());
        kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_TOPIC, event);

        kafkaLogProducer.logInfo(
                "OTP_SENT", java.util.Map.of("email", otpGenerateRequest.getEmail()));

        return true;
    }

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

    public void processVerification(String email) {
        log.info("Processing verification for email: {}", email);
        kafkaLogProducer.logInfo(
                "DIGILOCKER_VERIFICATION_STARTED", java.util.Map.of("email", email));

        // Generate dummy data
        DigiLockerDataFetchedEvent event =
                DigiLockerDataFetchedEvent.builder()
                        .email(email)
                        .aadharNumber(UUID.randomUUID().toString())
                        .panNumber(UUID.randomUUID().toString())
                        .firstName("John")
                        .lastName("Doe")
                        .middleName("Quincy")
                        .dob(RandomDate.generateRandomDOB())
                        .address(UUID.randomUUID().toString())
                        .additionalDetails(
                                new HashMap<>(Map.of("requestId", UUID.randomUUID().toString())))
                        .build();

        kafkaTemplate.send(KafkaTopicNameConstants.DIGILOCKER_DATA_FETCHED_TOPIC, email, event);

        kafkaLogProducer.logInfo(
                "DIGILOCKER_DATA_FETCHED",
                java.util.Map.of(
                        "email", email,
                        "aadharNumber", event.getAadharNumber(),
                        "panNumber", event.getPanNumber()));
    }

    // otp related helper functions
    private void deleteOtp(String key) {
        redisTemplate.delete(key);
    }

    private void saveOtp(String key, String otp) {
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(10));
    }
}
