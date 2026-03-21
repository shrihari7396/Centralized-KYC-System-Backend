package com.rbi.consent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbi.consent.client.BankFeignClient;
import com.rbi.consent.client.UserFeignClient;
import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.dto.UserDto;
import com.rbi.consent.entity.ConsentRecord;
import com.rbi.consent.enums.ConsentDocumentType;
import com.rbi.consent.enums.ConsentStatus;
import com.rbi.consent.event.ConsentApprovedEvent;
import com.rbi.consent.exception.ConsentExpiredException;
import com.rbi.consent.exception.ConsentNotFoundException;
import com.rbi.consent.exception.ConsentRejectedException;
import com.rbi.consent.kafka.ConsentKafkaProducer;
import com.rbi.consent.logging.KafkaLogProducer;
import com.rbi.consent.repository.ConsentRepository;
import com.rbi.consent.security.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentServiceImpl implements ConsentService {

    private final ConsentRepository consentRepository;
    private final JwtService jwtService;
    private final WebSocketPublisher webSocketPublisher;
    private final BankFeignClient bankFeignClient;
    private final UserFeignClient userFeignClient;
    private final ConsentKafkaProducer consentKafkaProducer;
    private final KafkaLogProducer kafkaLogProducer;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private static final String OTP_KEY_PREFIX = "consent_otp:";
    private static final long OTP_EXPIRY_MINUTES = 5;

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

        // Notify Frontend WebSocket
        webSocketPublisher.publishConsentRequested(requestDto.getUserId(), saved);
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
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));

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
            throw new RuntimeException("OTP expired or not requested.");
        }

        if (!storedOtp.equals(otpInput)) {
            kafkaLogProducer.logWarning(
                    "OTP_VERIFICATION_FAILED",
                    java.util.Map.of("consentId", consentId, "reason", "Invalid OTP provided"));
            throw new RuntimeException("Invalid OTP provided.");
        }

        // Delete OTP after successful verification
        redisTemplate.delete(redisKey);
    }

    @Override
    public ConsentResponseDto approveConsent(UUID consentId, String otp) {
        verifyOtp(consentId, otp);

        ConsentRecord record =
                consentRepository
                        .findById(consentId)
                        .orElseThrow(() -> new ConsentNotFoundException("Consent not found."));

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            throw new IllegalArgumentException("Consent is not in PENDING state.");
        }

        List<ConsentDocumentType> documents = fromJson(record.getConsentDocuments());
        String jwt =
                jwtService.generateConsentToken(
                        record.getUserId(), record.getBankId(), record.getId(), documents);

        record.setConsentStatus(ConsentStatus.APPROVED);
        record.setApprovedAt(LocalDateTime.now());
        record.setExpiryAt(LocalDateTime.now().plusDays(2));
        record.setActive(true);
        record.setJwtToken(jwt);

        ConsentRecord updated = consentRepository.save(record);

        // Notify Bank via Kafka Event
        ConsentApprovedEvent event =
                ConsentApprovedEvent.builder()
                        .consentId(updated.getId())
                        .userId(updated.getUserId())
                        .bankId(updated.getBankId())
                        .jwtToken(jwt)
                        .build();
        consentKafkaProducer.sendConsentApprovedEvent(event);

        // Notify WS success
        webSocketPublisher.publishConsentApproved(
                record.getBankId(), "Consent Approved for " + record.getUserId());

        kafkaLogProducer.logInfo(
                "CONSENT_APPROVED",
                java.util.Map.of(
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
                .jwtToken(updated.getJwtToken())
                .expiryAt(updated.getExpiryAt())
                .build();
    }

    @Override
    public List<ConsentResponseDto> getAllConsents(String userId) {
        return consentRepository.findByUserId(userId).stream()
                .map(
                        record ->
                                ConsentResponseDto.builder()
                                        .consentId(record.getId())
                                        .bankId(record.getBankId())
                                        .status(record.getConsentStatus())
                                        .jwtToken(record.getJwtToken())
                                        .expiryAt(record.getExpiryAt())
                                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void rejectConsent(UUID consentId, String otp) {
        verifyOtp(consentId, otp);

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

        // Notify Bank
        webSocketPublisher.publishConsentRejected(
                record.getBankId(), "Consent Rejected for user " + record.getUserId());

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
                .jwtToken(record.getJwtToken())
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

    private List<ConsentDocumentType> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConsentDocumentType>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize documents.");
        }
    }
}
