package org.rbi.consentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.rbi.consentservice.client.BankFeignClient;
import org.rbi.consentservice.client.UserFeignClient;
import org.rbi.consentservice.config.KafkaTopicNameConstants;
import org.rbi.consentservice.dto.*;
import org.rbi.consentservice.entity.ConsentRecord;
import org.rbi.consentservice.enums.ConsentDocumentType;
import org.rbi.consentservice.enums.ConsentStatus;
import org.rbi.consentservice.event.ConsentResponseEvent;
import org.rbi.consentservice.event.CreateOtpEvent;
import org.rbi.consentservice.event.DocumentType;
import org.rbi.consentservice.event.NotificationEvent;
import org.rbi.consentservice.exception.ConsentExpiredException;
import org.rbi.consentservice.exception.ConsentNotFoundException;
import org.rbi.consentservice.exception.ConsentRejectedException;
import org.rbi.consentservice.exception.InValidOtpException;
import org.rbi.consentservice.exception.PreviouslyRejectedConsentException;
import org.rbi.consentservice.kafka.ConsentKafkaProducer;
import org.rbi.consentservice.logging.KafkaLogProducer;
import org.rbi.consentservice.repository.ConsentRepository;
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
        // Fetch all records for this pair in ONE hit
        List<ConsentRecord> existingRecords =
                consentRepository.findByUserIdAndBankId(requestDto.getUserId(), requestDto.getBankId());

        // 1. De-activate active ones (using memory list)
        existingRecords.stream()
                .filter(ConsentRecord::isActive)
                .forEach(
                        existing -> {
                            existing.setActive(false);
                            existing.setConsentStatus(ConsentStatus.EXPIRED);
                            consentRepository.save(existing);
                            log.info(
                                    "De-activated existing active consent {} for user {}",
                                    existing.getId(),
                                    requestDto.getUserId());
                        });

        // 2. Save New Consent Record
        ConsentRecord consentRecord =
                ConsentRecord.builder()
                        .id(UUID.fromString(requestDto.getConsentId()))
                        .userId(requestDto.getUserId())
                        .bankId(requestDto.getBankId())
                        .consentStatus(ConsentStatus.PENDING)
                        .active(false)
                        .requestedAt(LocalDateTime.now())
                        .consentDocuments(toJson(requestDto.getRequestedDocuments()))
                        .build();

        ConsentRecord saved = consentRepository.save(consentRecord);

        // 2. Fetch User Details for correct email (Mandatory)
        UserDto user;
        try {
            user = userFeignClient.getUserDetailsById(requestDto.getUserId());
            if (user == null || user.getEmail() == null) {
                throw new ConsentNotFoundException("User details or email not found for ID: " + requestDto.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to fetch user details for userId {}: {}", requestDto.getUserId(), e.getMessage());
            throw new ConsentNotFoundException("Could not initiate consent: User details unavailable.");
        }

        String userEmail = user.getEmail();

        // 3. Fetch Bank Details for notification
        BankDto bank = null;
        try {
            bank = bankFeignClient.getBankById(requestDto.getBankId().toString());
        } catch (Exception e) {
            log.warn("Failed to fetch bank details for bankId {}: {}", requestDto.getBankId(), e.getMessage());
        }

        // 4. Trigger Global Notification Event via Kafka (WebSocket for Frontend)
        String nId = userEmail + "_" + requestDto.getBankId() + "_" + saved.getId();
        NotificationEvent notifEvent =
                NotificationEvent.builder()
                        .notificationId(nId)
                        .targetId(requestDto.getUserId()) // targetId is the UUID (userId)
                        .email(userEmail) // Actual email for identification
                        .type("CONSENT_REQUEST")
                        .message(
                                String.format(
                                        "New KYC Consent request from %s.",
                                        (bank != null ? bank.getName() : "a Bank")))
                        .timestamp(System.currentTimeMillis())
                        .actionUrl(consentBaseUrl + "/" + saved.getId() + "/status")
                        .metadata(
                                java.util.Map.of(
                                        "consentId", saved.getId().toString(),
                                        "bankId", requestDto.getBankId().toString(),
                                        "bankName", (bank != null ? bank.getName() : "Unknown"),
                                        "bankCode", (bank != null ? bank.getCode() : "N/A"),
                                        "email", userEmail))
                        .build();

        kafkaTemplate.send("notification-events-topic", notifEvent);
        log.info("Published NotificationEvent to Kafka for consent: {}", saved.getId());

        kafkaLogProducer.logInfo(
                "CONSENT_REQUESTED",
                Map.of(
                        "userId", requestDto.getUserId(),
                        "bankId", requestDto.getBankId().toString(),
                        "consentId", saved.getId().toString()));
    }

    private void triggerEmailOtp(ConsentRecord record, String otp, UserDto user) {
        String userEmail = user.getEmail();

        CreateOtpEvent otpEvent =
                CreateOtpEvent.builder()
                        .email(userEmail)
                        .otp(otp)
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .serviceType("CONSENT")
                        .build();

        kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_TOPIC, otpEvent);
        log.info("Sent CreateOtpEvent via Kafka to EmailService for consent {}", record.getId());
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

        if (record.getConsentStatus() == ConsentStatus.REJECTED) {
            kafkaLogProducer.logWarning(
                    "CONSENT_OTP_BLOCKED",
                    Map.of("consentId", consentId.toString(), "reason", "Previously Rejected"));
            throw new PreviouslyRejectedConsentException(
                    "You have previously rejected this consent. Further operations are blocked.");
        }

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            kafkaLogProducer.logWarning(
                    "CONSENT_OTP_REJECTED",
                    java.util.Map.of("consentId", consentId, "reason", "Not in pending state"));
            throw new IllegalArgumentException(
                    "OTP can only be sent for PENDING consent requests.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        // Store in Redis with TTL
        String redisKey = OTP_KEY_PREFIX + consentId;
        redisTemplate
                .opsForValue()
                .set(redisKey, otp, OTP_EXPIRY_MINUTES, java.util.concurrent.TimeUnit.MINUTES);

        log.info("Generated OTP for consent {}: {}", consentId, otp);

        // Fetch User details for correct delivery
        UserDto user;
        try {
            user = userFeignClient.getUserDetailsById(record.getUserId());
            if (user == null || user.getEmail() == null) {
                throw new ConsentNotFoundException("User email details unavailable for OTP.");
            }
        } catch (Exception e) {
            log.error("Failed to fetch user email for consent {}: {}", consentId, e.getMessage());
            throw new ConsentNotFoundException("Cannot send OTP: User details unavailable.");
        }

        triggerEmailOtp(record, otp, user);
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
        ConsentRecord record =
                consentRepository
                        .findById(consentId)
                        .orElseThrow(() -> new ConsentNotFoundException("Consent not found."));

        if (record.getConsentStatus() == ConsentStatus.REJECTED) {
            throw new PreviouslyRejectedConsentException(
                    "This consent was previously rejected and cannot be accepted later.");
        }

        verifyOtp(consentId, actionDto.getOtp());

        Action action = actionDto.getAction();
        if (action.equals(Action.ACCEPT)) {
            approveConsent(consentId);
        } else if (action.equals(Action.REJECT)) {
            rejectConsent(consentId);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    private ConsentResponseDto approveConsent(UUID consentId) {
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

    private void rejectConsent(UUID consentId) {
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
