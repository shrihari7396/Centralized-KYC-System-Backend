package org.kyc.management.bankservice.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.*;
import org.kyc.management.bankservice.entity.Bank;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.BankCustomerKycData;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.entity.DocumentType;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.CreateOtpEvent;
import org.kyc.management.bankservice.exception.*;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerMongoRepository;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.kyc.management.bankservice.repository.BankRepository;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.kyc.management.bankservice.security.JwtValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankServiceImpl implements BankService {

    private final BankCustomerRepository repo;
    private final KafkaLogProducer kafkaLogProducer;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
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
                "GET_CUSTOMER_STATUS", Map.of("userId", globalId, "bankId", bankId));
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

        if (c.getConsentIds() == null || c.getConsentIds().isEmpty()) {
            throw new ConsentNotFoundException("No consents found for customer");
        }
        String latestConsentId = c.getConsentIds().get(c.getConsentIds().size() - 1);
        Consent consent =
                consentRepo
                        .findById(latestConsentId)
                        .orElseThrow(() -> new ConsentNotFoundException("Consent not found"));
        consent.setStatus(KycStatus.ACTIVE_OVERRIDE);
        consent.setUpdatedAt(LocalDateTime.now());
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
        log.info("Comme: {}", bankRepo.findAll());
        if (!bankRepo.existsByUsername(otpRequestDto.getEmail())) {
            log.info("In side if");
            kafkaLogProducer.logError(
                    "LOGIN_REJECTED",
                    Map.of(
                            "username",
                            otpRequestDto.getEmail(),
                            "reason",
                            "Username does not exist in Bank DB"));
            throw new BankNotFoundException("Username is not registered with the Bank");
        }

        return generateAndSendOtp(
                otpRequestDto.getFirstName(),
                otpRequestDto.getLastName(),
                otpRequestDto.getEmail());
    }

    private Boolean generateAndSendOtp(String firstName, String lastName, String email) {
        int otp = secureRandom.nextInt(1_000_000);
        String finalotp = String.format("%06d", otp);
        log.info("Otp: {}", finalotp);
        CreateOtpEvent event =
                CreateOtpEvent.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .otp(finalotp)
                        .serviceType("BANK_LOGIN")
                        .build();

        saveOtp(finalotp, email);
        // Using string topic name to avoid circular dependencies or missing constants
        kafkaTemplate.send("OTP_CREATED_TOPIC", event);

        kafkaLogProducer.logInfo("OTP_SENT", java.util.Map.of("email", email));

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

        java.util.List<Consent> approvedConsents =
                new java.util.ArrayList<>(consentRepo.findByStatus(KycStatus.ACTIVE));
        approvedConsents.addAll(consentRepo.findByStatus(KycStatus.ACTIVE_OVERRIDE));

        java.util.List<String> userIds =
                approvedConsents.stream().map(Consent::getUserId).collect(Collectors.toList());

        return repo.findAll().stream()
                .filter(c -> userIds.contains(c.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> fetchKycData(String userId, String bankId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();

        // Insulation: Verify customer actually belongs to this bank
        repo.findByUserIdAndBankId(globalId, bankId)
                .orElseThrow(
                        () -> new CustomerNotFoundException("Customer not found for this bank"));

        kafkaLogProducer.logInfo(
                "FETCHING_KYC_DATA_LOCAL", java.util.Map.of("userId", userId, "bankId", bankId));

        BankCustomerKycData mongoDoc =
                mongoRepo
                        .findByUserIdAndBankId(globalId, bankId)
                        .orElseThrow(
                                () ->
                                        new KycDataNotFoundException(
                                                "KYC data not found in local DB yet."));

        return mongoDoc.getKycData();
    }

    @Override
    public Bank getBankProfile(String email) {
        return bankRepo.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Bank not found"));
    }

    @Override
    public List<BankCustomer> getCustomersByBank(String bankId) {
        return repo.findByBankId(bankId);
    }

    @Override
    public DashboardStatsResponse getDashboardStats(String bankId) {
        long total = repo.countByBankId(bankId);
        long approved =
                consentRepo.countByBankIdAndStatus(bankId, KycStatus.ACTIVE)
                        + consentRepo.countByBankIdAndStatus(bankId, KycStatus.ACTIVE_OVERRIDE);
        long pending =
                consentRepo.countByBankIdAndStatus(bankId, KycStatus.CONSENT_PENDING)
                        + consentRepo.countByBankIdAndStatus(bankId, KycStatus.KYC_PENDING);
        long rejected = consentRepo.countByBankIdAndStatus(bankId, KycStatus.KYC_REJECTED);
        long override = consentRepo.countByBankIdAndStatus(bankId, KycStatus.ACTIVE_OVERRIDE);

        Map<String, Long> riskDist =
                repo.findByBankId(bankId).stream()
                        .filter(c -> c.getRiskLevel() != null)
                        .collect(
                                Collectors.groupingBy(
                                        BankCustomer::getRiskLevel, Collectors.counting()));

        return DashboardStatsResponse.builder()
                .totalCustomers(total)
                .approvedCount(approved)
                .pendingCount(pending)
                .rejectedCount(rejected)
                .overrideCount(override)
                .riskDistribution(riskDist)
                .build();
    }

    @Override
    public List<Consent> getPendingConsents(String email) {
        Bank bank =
                bankRepo.findByUsername(email)
                        .orElseThrow(() -> new RuntimeException("Bank not found"));
        return consentRepo.findByBankIdAndStatus(bank.getId(), KycStatus.CONSENT_PENDING);
    }

    @Override
    public List<Consent> getAllConsentsByBank(String bankId) {
        return consentRepo.findByBankId(bankId);
    }

    @Override
    public List<Bank> getAllBanks() {
        return bankRepo.findAll();
    }

    @Override
    public Bank getBankById(String bankId) {
        return bankRepo.findById(bankId)
                .orElseThrow(() -> new BankNotFoundException("Bank not found for id: " + bankId));
    }

    @Override
    public List<BankCustomerKycData> getKycHistory(String userId, String bankId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();
        return mongoRepo.findByUserIdAndBankId(globalId, bankId).map(List::of).orElse(List.of());
    }

    @Override
    public Bank registerBank(BankRegisterRequest req) {
        Bank bank =
                Bank.builder()
                        .name(req.getName())
                        .code(req.getCode())
                        .username(req.getUsername())
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
        return bankRepo.save(bank);
    }

    @Override
    public List<TimelineEvent> getUnifiedTimeline(String userId, String bankId) {
        String globalId = userFeignClient.getUserDetails(userId).getUserId();
        List<TimelineEvent> timeline = new ArrayList<>();

        // 1. Add Consent status changes
        consentRepo
                .findByUserIdAndBankId(globalId, bankId)
                .ifPresent(
                        c -> {
                            timeline.add(
                                    TimelineEvent.builder()
                                            .timestamp(c.getCreatedAt())
                                            .type("CONSENT_CREATED")
                                            .message("KYC request initiated")
                                            .status(c.getStatus().name())
                                            .build());
                            if (c.getUpdatedAt() != null
                                    && !c.getUpdatedAt().equals(c.getCreatedAt())) {
                                timeline.add(
                                        TimelineEvent.builder()
                                                .timestamp(c.getUpdatedAt())
                                                .type("CONSENT_UPDATED")
                                                .message("Status changed to " + c.getStatus())
                                                .status(c.getStatus().name())
                                                .build());
                            }
                        });

        // 2. Add KYC Data fetch events
        mongoRepo
                .findByUserIdAndBankId(globalId, bankId)
                .ifPresent(
                        data -> {
                            timeline.add(
                                    TimelineEvent.builder()
                                            .timestamp(data.getCreatedAt())
                                            .type("DATA_FETCHED")
                                            .message("Detailed KYC document data retrieved")
                                            .status("FETCH_SUCCESS")
                                            .build());
                        });

        return timeline.stream()
                .sorted(Comparator.comparing(TimelineEvent::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public BankCustomer updateRiskLevel(RiskUpdateRequest request) {
        String globalId = userFeignClient.getUserDetails(request.getUserId()).getUserId();
        BankCustomer customer =
                repo.findByUserIdAndBankId(globalId, request.getBankId())
                        .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        customer.setRiskLevel(request.getNewRiskLevel());
        customer.setOverrideReason("Manual Reclassification: " + request.getReason());
        return repo.save(customer);
    }

    @Override
    public Page<BankCustomer> searchCustomers(
            String bankId, String query, KycStatus status, String riskLevel, Pageable pageable) {
        if (riskLevel != null && !riskLevel.isEmpty()) {
            return repo.findByBankIdAndUserIdRegexAndRiskLevel(
                    bankId, query != null ? query : "", riskLevel, pageable);
        }
        return repo.findByBankIdAndUserIdContaining(bankId, query != null ? query : "", pageable);
    }

    @Override
    public BankCustomerKycData findByConsentId(String consentId) {
        return mongoRepo
                .findByConsentId(consentId)
                .orElseThrow(() -> new KycDataNotFoundException("KYC data not found for consent"));
    }

    @Override
    public Map<DocumentType, String> getDetailReport(Long kycRequestId) {
        // Placeholder implementation - In a real system, this would fetch from a
        // VerificationService
        // or a detailed audit log entry.
        kafkaLogProducer.logInfo("GET_DETAIL_REPORT", Map.of("kycRequestId", kycRequestId));
        return Map.of(DocumentType.PAN, "Verified", DocumentType.AADHAAR, "Verified");
    }
}
