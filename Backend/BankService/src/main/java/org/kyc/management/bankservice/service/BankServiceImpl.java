package org.kyc.management.bankservice.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.LoginResponseDto;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.BankCustomerKycData;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.CreateOtpEvent;
import org.kyc.management.bankservice.exception.InValidOtpException;
import org.kyc.management.bankservice.exception.CustomerNotFoundException;
import org.kyc.management.bankservice.exception.ConsentNotFoundException;
import org.kyc.management.bankservice.exception.KycDataNotFoundException;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerMongoRepository;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.repository.BankRepository;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.kyc.management.bankservice.security.JwtValidator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final BankCustomerRepository repo;
    private final KafkaLogProducer kafkaLogProducer;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;
    private final SecureRandom secureRandom;
    private final JwtValidator jwtValidator;
    private final BankCustomerMongoRepository mongoRepo;
    private final UserFeignClient userFeignClient;
    private final BankRepository bankRepo;
    private final ConsentRepository consentRepo;

    @Override
    public BankCustomer getCustomerStatus(String userId, String bankId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();
        kafkaLogProducer.logInfo(
                "GET_CUSTOMER_STATUS",
                Map.of("userId", globalId, "bankId", bankId));
        return repo.findByUserIdAndBankId(globalId, bankId)
                .orElseThrow(
                        () -> {
                            kafkaLogProducer.logError(
                                    "CUSTOMER_NOT_FOUND",
                                    java.util.Map.of("userId", userId, "bankId", bankId));
                            return new CustomerNotFoundException("Customer not found");
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
                                    return new CustomerNotFoundException("Customer not found");
                                });

        if ("HIGH_RISK".equalsIgnoreCase(c.getRiskLevel())) {
            kafkaLogProducer.logWarning(
                    "BYPASS_KYC_REJECTED",
                    Map.of("userId", req.getUserId(), "reason", "HIGH RISK override attempted"));
            throw new RuntimeException("Cannot bypass KYC for HIGH_RISK user");
        }

        Consent consent = consentRepo.findById(c.getConsentId())
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found"));
        consent.setStatus(KycStatus.ACTIVE_OVERRIDE);
        consentRepo.save(consent);

        c.setOverrideReason(req.getReason());
        c.setVerifiedAt(LocalDateTime.now());
        kafkaLogProducer.logInfo(
                "BYPASS_KYC_SUCCESS",
                Map.of("userId", req.getUserId(), "overrideReason", req.getReason()));
        return repo.save(c);
    }

    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        if (!bankRepo.existsByUsername(otpRequestDto.getEmail())) {
            kafkaLogProducer.logError(
                    "LOGIN_REJECTED",
                    Map.of(
                            "username",
                            otpRequestDto.getEmail(),
                            "reason",
                            "Username does not exist in Bank DB"));
            throw new RuntimeException("Username is not registered with the Bank");
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
                    "LOGIN_FAILED", Map.of("email", otpVerificationDto.getEmail()));
            throw new InValidOtpException("Invalid OTP");
        }

        kafkaLogProducer.logInfo(
                "USER_LOGGED_IN", Map.of("email", response.getEmail(), "role", response.getRole()));
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
    public List<BankCustomer> getApprovedConsents() {
        kafkaLogProducer.logInfo(
                "GET_APPROVED_CONSENTS",
                java.util.Map.of("action", "Listing users with approved consents"));
        
        java.util.List<Consent> approvedConsents = new java.util.ArrayList<>(consentRepo.findByStatus(KycStatus.ACTIVE));
        approvedConsents.addAll(consentRepo.findByStatus(KycStatus.ACTIVE_OVERRIDE));

        java.util.List<String> userIds = approvedConsents.stream()
                .map(Consent::getUserId)
                .collect(Collectors.toList());

        return repo.findAll().stream()
                .filter(c -> userIds.contains(c.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> fetchKycData(String userId, String bankId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();
        
        // Insulation: Verify customer actually belongs to this bank
        repo.findByUserIdAndBankId(globalId, bankId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for this bank"));

        kafkaLogProducer.logInfo(
                "FETCHING_KYC_DATA_LOCAL",
                java.util.Map.of("userId", userId, "bankId", bankId));
        
        BankCustomerKycData mongoDoc = mongoRepo.findByUserIdAndBankId(globalId, bankId)
                .orElseThrow(() -> new KycDataNotFoundException("KYC data not found in local DB yet."));

        return mongoDoc.getKycData();
    }
}
