package org.rbi.kycmanagementservice.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.kycmanagementservice.configs.KafkaConstants;
import org.rbi.kycmanagementservice.dto.DocumentType;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.rbi.kycmanagementservice.enums.ConsentStatus;
import org.rbi.kycmanagementservice.event.ConsentResponseEvent;
import org.rbi.kycmanagementservice.event.KycDataEvent;
import org.rbi.kycmanagementservice.repository.KycRecordRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentResponseConsumer {

    private final KycRecordRepository kycRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConstants.CONSENT_RESPONSE_TOPIC, groupId = "kyc-management-group")
    public void consumeConsentResponse(ConsentResponseEvent event) {
        log.info(
                "Received ConsentResponseEvent for userId: {} and bankId: {}",
                event.getUserId(),
                event.getBankId());

        if (event.getStatus() == ConsentStatus.APPROVED) {
            Optional<KycRecord> recordOpt = kycRecordRepository.findByUserId(event.getUserId());
            if (recordOpt.isPresent()) {
                KycRecord record = recordOpt.get();

                Map<String, Object> kycData = new HashMap<>();
                if (event.getRequestedDocuments() != null) {
                    if (event.getRequestedDocuments().contains(DocumentType.AADHAAR)) {
                        kycData.put("aadhaarNo", record.getAadharNumber());
                    }
                    if (event.getRequestedDocuments().contains(DocumentType.PAN)) {
                        kycData.put("pan", record.getPanNumber());
                    }
                    if (record.getAddress() != null
                            && event.getRequestedDocuments().contains(DocumentType.ADDRESS_PROOF)) {
                        kycData.put("address", record.getAddress());
                    }
                } else {
                    // Fallback: Add all if nothing is filtered
                    kycData.put("aadhaarNo", record.getAadharNumber());
                    kycData.put("pan", record.getPanNumber());
                    if (record.getAddress() != null) {
                        kycData.put("address", record.getAddress());
                    }
                }

                KycDataEvent dataEvent =
                        KycDataEvent.builder()
                                .userId(event.getUserId())
                                .bankId(event.getBankId())
                                .consentId(event.getConsentId())
                                .kycData(kycData)
                                .build();

                kafkaTemplate.send(KafkaConstants.KYC_DATA_TOPIC, dataEvent);
                log.info(
                        "Published KycDataEvent to kyc-data-topic for userId: {}",
                        event.getUserId());
            } else {
                log.warn("KycRecord not found for userId: {}", event.getUserId());
            }
        }
    }
}
