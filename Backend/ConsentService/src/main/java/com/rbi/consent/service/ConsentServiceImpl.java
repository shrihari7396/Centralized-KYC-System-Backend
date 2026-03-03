package com.rbi.consent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbi.consent.client.BankFeignClient;
import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.entity.ConsentRecord;
import com.rbi.consent.enums.ConsentDocumentType;
import com.rbi.consent.enums.ConsentStatus;
import com.rbi.consent.exception.ConsentExpiredException;
import com.rbi.consent.exception.ConsentNotFoundException;
import com.rbi.consent.exception.ConsentRejectedException;
import com.rbi.consent.repository.ConsentRepository;
import com.rbi.consent.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentServiceImpl implements ConsentService {

    private final ConsentRepository consentRepository;
    private final JwtService jwtService;
    private final WebSocketPublisher webSocketPublisher;
    private final BankFeignClient bankFeignClient;
    private final ObjectMapper objectMapper;

    @Override
    public void requestConsent(ConsentRequestDto requestDto) {
        // If an active consent already exists, maybe reject or override. 
        // For now, we just create a new one.
        
        ConsentRecord consentRecord = ConsentRecord.builder()
                .userId(requestDto.getUserId())
                .bankId(requestDto.getBankId())
                .consentStatus(ConsentStatus.PENDING)
                .active(false)
                .requestedAt(LocalDateTime.now())
                .consentDocuments(toJson(requestDto.getRequestedDocuments()))
                .build();

        ConsentRecord saved = consentRepository.save(consentRecord);

        // Notify Frontend WebSocket
        webSocketPublisher.publishConsentRequested(requestDto.getUserId(), saved);
    }

    @Override
    public ConsentResponseDto approveConsent(UUID consentId) {
        ConsentRecord record = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found."));

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            throw new IllegalArgumentException("Consent is not in PENDING state.");
        }

        List<ConsentDocumentType> documents = fromJson(record.getConsentDocuments());
        String jwt = jwtService.generateConsentToken(record.getUserId(), record.getBankId(), record.getId(), documents);

        record.setConsentStatus(ConsentStatus.APPROVED);
        record.setApprovedAt(LocalDateTime.now());
        record.setExpiryAt(LocalDateTime.now().plusDays(2));
        record.setActive(true);
        record.setJwtToken(jwt);

        ConsentRecord updated = consentRepository.save(record);

        // Notify Bank
        try {
            bankFeignClient.notifyBank(updated.getId(), jwt);
        } catch (Exception e) {
            log.error("Failed to notify bank: {}", e.getMessage());
            // Optionally handle retry or queueing
        }

        // Notify WS success
        webSocketPublisher.publishConsentApproved(record.getBankId(), "Consent Approved for " + record.getUserId());

        return ConsentResponseDto.builder()
                .consentId(updated.getId())
                .status(updated.getConsentStatus())
                .jwtToken(updated.getJwtToken())
                .expiryAt(updated.getExpiryAt())
                .build();
    }

    @Override
    public void rejectConsent(UUID consentId) {
        ConsentRecord record = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found."));

        if (record.getConsentStatus() != ConsentStatus.PENDING) {
            throw new IllegalArgumentException("Consent is not in PENDING state.");
        }

        record.setConsentStatus(ConsentStatus.REJECTED);
        record.setActive(false);
        consentRepository.save(record);

        // Notify Bank
        webSocketPublisher.publishConsentRejected(record.getBankId(), "Consent Rejected for user " + record.getUserId());
    }

    @Override
    public ConsentResponseDto getConsent(UUID userId, UUID bankId) {
        Optional<ConsentRecord> consentOpt = consentRepository.findByUserIdAndBankIdAndActiveTrue(userId, bankId);

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
