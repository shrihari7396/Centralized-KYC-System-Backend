package org.kyc.management.bankservice.service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.LoginResponseDto;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.CreateOtpEvent;
import org.kyc.management.bankservice.exception.InValidOtpException;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.kyc.management.bankservice.security.JwtValidator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final BankCustomerRepository repo;
    private final KafkaLogProducer kafkaLogProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;
    private final SecureRandom secureRandom;
    private final JwtValidator jwtValidator;

    @Override
    public BankCustomer getCustomerStatus(String userId) {
        kafkaLogProducer.logInfo("GET_CUSTOMER_STATUS", java.util.Map.of("userId", userId));
        return repo.findByUserId(userId)
                .orElseThrow(
                        () -> {
                            kafkaLogProducer.logError(
                                    "CUSTOMER_NOT_FOUND", java.util.Map.of("userId", userId));
                            return new RuntimeException("Customer not found");
                        });
    }

    @Override
    public BankCustomer bypassKyc(BypassRequestDto req) {
        BankCustomer c =
                repo.findByUserId(req.getUserId())
                        .orElseThrow(
                                () -> {
                                    kafkaLogProducer.logError(
                                            "BYPASS_KYC_FAILED",
                                            java.util.Map.of(
                                                    "userId",
                                                    req.getUserId(),
                                                    "reason",
                                                    "Customer not found"));
                                    return new RuntimeException("Customer not found");
                                });

        if ("HIGH_RISK".equalsIgnoreCase(c.getRiskLevel())) {
            kafkaLogProducer.logWarning(
                    "BYPASS_KYC_REJECTED",
                    java.util.Map.of(
                            "userId", req.getUserId(), "reason", "HIGH RISK override attempted"));
            throw new RuntimeException("Cannot bypass KYC for HIGH_RISK user");
        }

        c.setKycStatus(KycStatus.VERIFIED_OVERRIDE);
        c.setOverrideReason(req.getReason());
        c.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        kafkaLogProducer.logInfo(
                "BYPASS_KYC_SUCCESS",
                java.util.Map.of("userId", req.getUserId(), "overrideReason", req.getReason()));
        return repo.save(c);
    }

    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        return generateAndSendOtp(
                otpRequestDto.getFirstName(),
                otpRequestDto.getLastName(),
                otpRequestDto.getEmail(),
                otpRequestDto.getEmail());
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
                        .serviceType("BANK_LOGIN")
                        .build();

        saveOtp(finalotp, key);
        // Using string topic name to avoid circular dependencies or missing constants
        kafkaTemplate.send("OTP_CREATED_TOPIC", event);

        kafkaLogProducer.logInfo("OTP_SENT", java.util.Map.of("email", email, "key", key));

        return true;
    }

    @Override
    public Boolean verifyOtp(String otp, String key) {
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp == null) {
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED_EXPIRED", java.util.Map.of("key", key));
            throw new InValidOtpException("Expired or invalid OTP");
        }
        boolean result = storedOtp.equals(otp);
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
    public LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto) {
        LoginResponseDto response = null;

        if (verifyOtp(otpVerificationDto.getOtp(), otpVerificationDto.getEmail())) {
            String token =
                    jwtValidator.generateToken(
                            otpVerificationDto.getEmail(), otpVerificationDto.getRole());
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
            String jti = jwtValidator.getJtiFromToken(token);
            java.util.Date expiration = jwtValidator.getExpirationFromToken(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                redisTemplate.opsForValue().set("blacklist:" + jti, "true", Duration.ofMillis(ttl));
                return true;
            }
        } catch (Exception e) {
            kafkaLogProducer.logError("LOGOUT_FAILED", java.util.Map.of("token_snippet", token.substring(0, Math.min(token.length(), 10))));
        }
        return false;
    }
}
