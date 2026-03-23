package org.kyc.management.kycmanagementservice.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.kycmanagementservice.configs.KafkaConstants;
import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.kyc.management.kycmanagementservice.enums.ConsentStatus;
import org.kyc.management.kycmanagementservice.event.ConsentResponseEvent;
import org.kyc.management.kycmanagementservice.event.KycDataEvent;
import org.kyc.management.kycmanagementservice.repository.KycRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentResponseConsumer {

    private final KycRepository kycRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConstants.CONSENT_RESPONSE_TOPIC, groupId = "kyc-management-group")
    public void consumeConsentResponse(ConsentResponseEvent event) {
        log.info(
                "Received ConsentResponseEvent for userId: {} and bankId: {}",
                event.getUserId(),
                event.getBankId());

        if (event.getStatus() == ConsentStatus.APPROVED) {
            Optional<KycEntity> kycOpt = kycRepository.findById(event.getUserId());
            if (kycOpt.isPresent()) {
                KycEntity kyc = kycOpt.get();

                Map<String, Object> kycData = new HashMap<>();
                if (event.getRequestedDocuments() != null) {
                    if (event.getRequestedDocuments()
                            .contains(
                                    org.kyc.management.kycmanagementservice.dto.DocumentType
                                            .AADHAAR)) {
                        kycData.put("aadhaarNo", kyc.getAadhaarNo());
                    }
                    if (event.getRequestedDocuments()
                            .contains(
                                    org.kyc.management.kycmanagementservice.dto.DocumentType.PAN)) {
                        kycData.put("pan", kyc.getPan());
                    }
                    if (event.getRequestedDocuments()
                            .contains(
                                    org.kyc.management.kycmanagementservice.dto.DocumentType
                                            .DRIVING_LICENSE)) {
                        kycData.put("drivingLicense", kyc.getDrivingLicense());
                    }
                    if (kyc.getAddress() != null
                            && event.getRequestedDocuments()
                                    .contains(
                                            org.kyc.management.kycmanagementservice.dto.DocumentType
                                                    .ADDRESS_PROOF)) {
                        kycData.put("address", kyc.getAddress());
                    }
                } else {
                    // Fallback: Add all if nothing is filtered
                    kycData.put("aadhaarNo", kyc.getAadhaarNo());
                    kycData.put("pan", kyc.getPan());
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
                log.warn("KycEntity not found for userId: {}", event.getUserId());
            }
        }
    }
}
