package com.rbi.consent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbi.consent.client.BankFeignClient;
import com.rbi.consent.client.UserFeignClient;
import com.rbi.consent.dto.BankDto;
import com.rbi.consent.dto.ConsentActionDto;
import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.dto.UserDto;
import com.rbi.consent.entity.ConsentRecord;
import com.rbi.consent.enums.ConsentDocumentType;
import com.rbi.consent.enums.ConsentStatus;
import com.rbi.consent.event.ConsentResponseEvent;
import com.rbi.consent.event.DocumentType;
import com.rbi.consent.event.NotificationEvent;
import com.rbi.consent.exception.ConsentExpiredException;
import com.rbi.consent.exception.ConsentNotFoundException;
import com.rbi.consent.exception.ConsentRejectedException;
import com.rbi.consent.exception.InValidOtpException;
import com.rbi.consent.kafka.ConsentKafkaProducer;
import com.rbi.consent.logging.KafkaLogProducer;
import com.rbi.consent.repository.ConsentRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentServiceImpl implements ConsentService {

    private final ConsentRepository consentRepository;
    private final UserFeignClient userFeignClient;
    private final BankFeignClient bankFeignClient;
    private final ConsentKafkaProducer consentKafkaProducer;
    private final KafkaLogProducer kafkaLogProducer;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SecureRandom secureRandom;

    @Value("${app.consent-url}")
    private String consentBaseUrl;

    private static final String OTP_KEY_PREFIX = "consent_otp:";
    private static final long OTP_EXPIRY_MINUTES = 10;

    @Override
    public void requestConsent(ConsentRequestDto requestDto) {
        // If an active consent already exists, de-activate it before creating a new one.
        consentRepository
                .findByUserIdAndBankIdAndActiveTrue(requestDto.getUserId(), requestDto.getBankId())
                .ifPresent(
                        existing -> {
                            existing.setActive(false);
                            existing.setConsentStatus(ConsentStatus.EXPIRED);
                            consentRepository.save(existing);
                            log.info(
                                    "De-activated existing active consent {} for user {}",
                                    existing.getId(),
                                    requestDto.getUserId());
                        });

        ConsentRecord consentRecord =
                ConsentRecord.builder()
                        .userId(requestDto.getUserId())
                        .bankId(requestDto.getBankId())
                        .consentStatus(ConsentStatus.PENDING)
                        .active(false)
                        .requestedAt(LocalDateTime.now())
                        .consentDocuments(toJson(requestDto.getRequestedDocuments()))
                        .build();

        ConsentRecord saved = consentRepository.save(consentRecord);

        kafkaLogProducer.logInfo(
                "CONSENT_REQUESTED",
                java.util.Map.of(
                        "userId",
                        requestDto.getUserId(),
                        "bankId",
                        requestDto.getBankId(),
                        "consentId",
                        saved.getId()));

        // Trigger Global Notification Event via Kafka
        String nId = requestDto.getUserId() + "_" + requestDto.getBankId() + "_" + saved.getId();
        NotificationEvent notifEvent =
                com.rbi.consent.event.NotificationEvent.builder()
                        .notificationId(nId)
                        .targetId(requestDto.getUserId())
                        .email(requestDto.getUserId())
                        .type("CONSENT_REQUEST")
                        .message(
                                "A new KYC Consent request was initiated by a Bank. Action URL: "
                                        + consentBaseUrl
                                        + "/"
                                        + saved.getId()
                                        + "/status?Accept or Decline")
                        .timestamp(System.currentTimeMillis())
                        .actionUrl(
                                consentBaseUrl + "/" + saved.getId() + "/status?Accept or Decline")
                        .metadata(
                                java.util.Map.of(
                                        "consentId",
                                        saved.getId(),
                                        "bankId",
                                        requestDto.getBankId()))
                        .build();

        kafkaTemplate.send("notification-events-topic", notifEvent);
        log.info("Published universal NotificationEvent to Kafka for consent: {}", saved.getId());

        // Automatically trigger OTP generation and email-out for the target user-identifier
        sendConsentOtp(saved.getId());
    }

    @Override
    public void sendConsentOtp(UUID consentId) {
        ConsentRecord record =
                consentRepository
                        .findById(consentId)
                        .orElseThrow(
                                () -> {
                                    kafkaLogProducer.logError(
                                            "CONSENT_OTP_FAILED",
                                            java.util.Map.of(
                                                    "consentId",
                                                    consentId,
                                                    "reason",
                                                    "Consent not found"));
                                    return new ConsentNotFoundException("Consent not found.");
                                });

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            kafkaLogProducer.logWarning(
                    "CONSENT_OTP_REJECTED",
                    java.util.Map.of("consentId", consentId, "reason", "Not in pending state"));
            throw new IllegalArgumentException(
                    "OTP can only be sent for PENDING consent requests.");
        }

        // Fetch User Details for personalized OTP email
        UserDto user = null;
        try {
            user = userFeignClient.getUserDetails(record.getUserId());
        } catch (Exception e) {
            log.warn("Failed to fetch user details for consent {}: {}", consentId, e.getMessage());
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        // Store in Redis with TTL
        String redisKey = OTP_KEY_PREFIX + consentId;
        redisTemplate
                .opsForValue()
                .set(redisKey, otp, OTP_EXPIRY_MINUTES, java.util.concurrent.TimeUnit.MINUTES);

        log.info("Generated OTP for consent {}: {}", consentId, otp);

        // Send OTP via Kafka for EmailService to pick up
        com.rbi.consent.event.CreateOtpEvent otpEvent =
                com.rbi.consent.event.CreateOtpEvent.builder()
                        .email(record.getUserId()) // userId is the email
                        .otp(otp)
                        .firstName(user != null ? user.getFirstName() : "User")
                        .lastName(user != null ? user.getLastName() : "")
                        .serviceType("CONSENT")
                        .build();

        kafkaTemplate.send(
                com.rbi.consent.config.KafkaTopicNameConstants.OTP_CREATED_TOPIC, otpEvent);
        log.info("Sent CreateOtpEvent for consent {} to Kafka", consentId);
    }

    private void verifyOtp(UUID consentId, String otpInput) {
        String redisKey = OTP_KEY_PREFIX + consentId;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            kafkaLogProducer.logWarning(
                    "OTP_EXPIRED_OR_NOT_REQUESTED", java.util.Map.of("consentId", consentId));
            throw new InValidOtpException("Expired or invalid OTP");
        }

        if (!storedOtp.equals(otpInput)) {
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED",
                    java.util.Map.of("consentId", consentId, "reason", "Invalid OTP provided"));
            throw new InValidOtpException("Invalid OTP");
        }

        // Delete OTP after successful verification
        redisTemplate.delete(redisKey);
    }

    @Override
    public void submitConsent(UUID consentId, ConsentActionDto actionDto) {
        verifyOtp(consentId, actionDto.getOtp());

        String action = actionDto.getAction();
        if ("Accept".equalsIgnoreCase(action) || "Approved".equalsIgnoreCase(action)) {
            approveConsent(consentId);
        } else if ("Decline".equalsIgnoreCase(action) || "Reject".equalsIgnoreCase(action)) {
            rejectConsent(consentId);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    @Override
    public ConsentResponseDto approveConsent(UUID consentId) {
        ConsentRecord record =
                consentRepository
                        .findById(consentId)
                        .orElseThrow(() -> new ConsentNotFoundException("Consent not found."));

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            throw new IllegalArgumentException("Consent is not in PENDING state.");
        }

        record.setConsentStatus(ConsentStatus.APPROVED);
        record.setApprovedAt(LocalDateTime.now());
        record.setExpiryAt(LocalDateTime.now().plusDays(2));
        record.setActive(true);

        ConsentRecord updated = consentRepository.save(record);

        List<DocumentType> docs = new ArrayList<>();
        try {
            if (updated.getConsentDocuments() != null) {
                docs =
                        objectMapper.readValue(
                                updated.getConsentDocuments(),
                                new TypeReference<List<DocumentType>>() {});
            }
        } catch (Exception e) {
            log.error("Error parsing consent documents", e);
        }

        // Notify Bank via Kafka Event
        ConsentResponseEvent event =
                ConsentResponseEvent.builder()
                        .consentId(updated.getId())
                        .userId(updated.getUserId())
                        .bankId(updated.getBankId())
                        .status(ConsentStatus.APPROVED)
                        .requestedDocuments(docs)
                        .build();
        consentKafkaProducer.sendConsentResponseEvent(event);

        // Emit Universal Notification to the Bank
        NotificationEvent bankNotif =
                NotificationEvent.builder()
                        .notificationId(updated.getBankId() + "_" + updated.getId() + "_approved")
                        .targetId(updated.getBankId().toString())
                        .type("BANK_CONSENT_APPROVED")
                        .message("Consent Approved for " + updated.getUserId())
                        .timestamp(System.currentTimeMillis())
                        .build();
        kafkaTemplate.send("notification-events-topic", bankNotif);

        kafkaLogProducer.logInfo(
                "CONSENT_APPROVED",
                Map.of(
                        "consentId",
                        updated.getId(),
                        "userId",
                        updated.getUserId(),
                        "bankId",
                        updated.getBankId()));

        return ConsentResponseDto.builder()
                .consentId(updated.getId())
                .bankId(updated.getBankId())
                .status(updated.getConsentStatus())
                .expiryAt(updated.getExpiryAt())
                .build();
    }

    @Override
    public List<ConsentResponseDto> getAllConsents(String userId) {
        List<ConsentRecord> records = consentRepository.findByUserId(userId);
        Map<String, BankDto> bankById =
                records.stream()
                        .map(ConsentRecord::getBankId)
                        .distinct()
                        .map(
                                bankId -> {
                                    try {
                                        return bankFeignClient.getBankById(bankId.toString());
                                    } catch (Exception e) {
                                        log.warn(
                                                "Failed to fetch bank details for bankId {}: {}",
                                                bankId,
                                                e.getMessage());
                                        return null;
                                    }
                                })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toMap(BankDto::getId, Function.identity()));

        return records.stream()
                .map(
                        record ->
                                ConsentResponseDto.builder()
                                        .consentId(record.getId())
                                        .bankId(record.getBankId())
                                        .status(record.getConsentStatus())
                                        .expiryAt(record.getExpiryAt())
                                        .bank(bankById.get(record.getBankId().toString()))
                                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void rejectConsent(UUID consentId) {
        ConsentRecord record =
                consentRepository
                        .findById(consentId)
                        .orElseThrow(() -> new ConsentNotFoundException("Consent not found."));

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            throw new IllegalArgumentException("Consent is not in PENDING state.");
        }

        record.setConsentStatus(ConsentStatus.REJECTED);
        record.setActive(false);
        consentRepository.save(record);

        // Emit Universal Notification to the Bank
        NotificationEvent bankNotif =
                NotificationEvent.builder()
                        .notificationId(record.getBankId() + "_" + record.getId() + "_rejected")
                        .targetId(record.getBankId().toString())
                        .type("BANK_CONSENT_REJECTED")
                        .message("Consent Rejected for user " + record.getUserId())
                        .timestamp(System.currentTimeMillis())
                        .build();
        kafkaTemplate.send("notification-events-topic", bankNotif);

        List<DocumentType> docs = new ArrayList<>();
        try {
            if (record.getConsentDocuments() != null) {
                docs =
                        objectMapper.readValue(
                                record.getConsentDocuments(),
                                new TypeReference<List<DocumentType>>() {});
            }
        } catch (Exception e) {
            log.error("Error parsing consent documents", e);
        }

        // Notify Bank via Kafka Event for Rejection
        ConsentResponseEvent event =
                ConsentResponseEvent.builder()
                        .consentId(record.getId())
                        .userId(record.getUserId())
                        .bankId(record.getBankId())
                        .status(ConsentStatus.REJECTED)
                        .requestedDocuments(docs)
                        .build();
        consentKafkaProducer.sendConsentResponseEvent(event);

        kafkaLogProducer.logWarning(
                "CONSENT_REJECTED",
                java.util.Map.of(
                        "consentId",
                        record.getId(),
                        "userId",
                        record.getUserId(),
                        "bankId",
                        record.getBankId()));
    }

    @Override
    public ConsentResponseDto getConsent(String userId, UUID bankId) {
        Optional<ConsentRecord> consentOpt =
                consentRepository.findByUserIdAndBankIdAndActiveTrue(userId, bankId);

        if (consentOpt.isEmpty()) {
            // Check if rejected or expired
            throw new ConsentNotFoundException("No active consent found.");
        }

        ConsentRecord record = consentOpt.get();

        if (record.getExpiryAt().isBefore(LocalDateTime.now())) {
            record.setConsentStatus(ConsentStatus.EXPIRED);
            record.setActive(false);
            consentRepository.save(record);
            throw new ConsentExpiredException("Consent has expired.");
        }

        if (record.getConsentStatus() == ConsentStatus.REJECTED) {
            throw new ConsentRejectedException("Consent was rejected.");
        }

        return ConsentResponseDto.builder()
                .consentId(record.getId())
                .bankId(record.getBankId())
                .status(record.getConsentStatus())
                .expiryAt(record.getExpiryAt())
                .build();
    }

    private String toJson(List<ConsentDocumentType> types) {
        try {
            return objectMapper.writeValueAsString(types);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize documents.");
        }
    }
}
