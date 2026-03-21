package org.kyc.management.bankservice.service;

import java.security.SecureRandom;
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
    private final org.kyc.management.bankservice.client.KycFeignClient kycClient;
    private final org.kyc.management.bankservice.repository.BankCustomerMongoRepository mongoRepo;
    private final org.kyc.management.bankservice.client.UserFeignClient userFeignClient;

    @Override
    public BankCustomer getCustomerStatus(String userId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();
        kafkaLogProducer.logInfo("GET_CUSTOMER_STATUS", java.util.Map.of("userId", globalId));
        return repo.findByUserId(globalId)
                .orElseThrow(
                        () -> {
                            kafkaLogProducer.logError(
                                    "CUSTOMER_NOT_FOUND", java.util.Map.of("userId", userId));
                            return new RuntimeException("Customer not found");
                        });
    }

    @Override
    public BankCustomer bypassKyc(BypassRequestDto req) {
        String globalId = userFeignClient.getUserDetails(req.getUserId()).getUserId();
        BankCustomer c =
                repo.findByUserId(globalId)
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
        c.setVerifiedAt(java.time.LocalDateTime.now());
        kafkaLogProducer.logInfo(
                "BYPASS_KYC_SUCCESS",
                java.util.Map.of("userId", req.getUserId(), "overrideReason", req.getReason()));
        return repo.save(c);
    }

    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        if (!repo.existsByUserId(otpRequestDto.getEmail())) {
            kafkaLogProducer.logError(
                    "LOGIN_REJECTED",
                    java.util.Map.of(
                            "email",
                            otpRequestDto.getEmail(),
                            "reason",
                            "Email does not exist in Bank Customer DB"));
            throw new RuntimeException("Email address is not registered with the Bank");
        }

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
            kafkaLogProducer.logError(
                    "LOGOUT_FAILED",
                    java.util.Map.of(
                            "token_snippet", token.substring(0, Math.min(token.length(), 10))));
        }
        return false;
    }

    @Override
    public java.util.List<BankCustomer> getApprovedConsents() {
        kafkaLogProducer.logInfo(
                "GET_APPROVED_CONSENTS",
                java.util.Map.of("action", "Listing users with jwt tokens"));
        return repo.findAll().stream()
                .filter(c -> c.getConsentToken() != null)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.Map<String, Object> fetchKycData(String userId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();
        BankCustomer customer =
                repo.findByUserId(globalId)
                        .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getConsentToken() == null) {
            kafkaLogProducer.logError(
                    "FETCH_KYC_FAILED",
                    java.util.Map.of("userId", userId, "reason", "No Consent Token"));
            throw new RuntimeException("No approved consent token available for this user.");
        }

        kafkaLogProducer.logInfo("FETCHING_KYC_DATA", java.util.Map.of("userId", userId));
        java.util.Map<String, Object> data =
                kycClient.fetchKycData("Bearer " + customer.getConsentToken());

        org.kyc.management.bankservice.entity.BankCustomerKycData mongoDoc =
                org.kyc.management.bankservice.entity.BankCustomerKycData.builder()
                        .consentId(customer.getId().toString())
                        .bankCustomerEmail(globalId)
                        .kycData(data)
                        .fetchedAt(java.time.LocalDateTime.now())
                        .build();

        mongoRepo.save(mongoDoc);
        kafkaLogProducer.logInfo(
                "SAVED_KYC_MONGO",
                java.util.Map.of(
                        "userId",
                        userId,
                        "mongoId",
                        mongoDoc.getId() != null ? mongoDoc.getId() : "generated"));

        customer.setKycStatus(KycStatus.VERIFIED);
        repo.save(customer);

        return data;
    }
}
