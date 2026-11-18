package org.rbi.kycmanagementservice.consumer;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.kycmanagementservice.configs.KafkaConstants;
import org.rbi.kycmanagementservice.entity.KycEntity;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.rbi.kycmanagementservice.event.DigiLockerDataFetchedEvent;
import org.rbi.kycmanagementservice.event.KycVerificationSuccessEvent;
import org.rbi.kycmanagementservice.logging.KafkaLogProducer;
import org.rbi.kycmanagementservice.repository.KycRecordRepository;
import org.rbi.kycmanagementservice.repository.KycRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycKafkaConsumer {

    private final KycRecordRepository kycRecordRepository;
    private final KycRepository kycRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaLogProducer kafkaLogProducer;

    private static final String FETCHED_TOPIC = KafkaConstants.DIGILOCKER_DATA_FETCHED_TOPIC;
    private static final String SUCCESS_TOPIC = KafkaConstants.KYC_VERIFICATION_SUCCESS;
    private static final String KAFKA_GROUP_ID = KafkaConstants.KAFKA_GROUP_ID;

    @KafkaListener(topics = FETCHED_TOPIC, groupId = KAFKA_GROUP_ID)
    public void consumeDigilockerData(DigiLockerDataFetchedEvent event) {
        log.info("Consume digilocker data received for userId: {}", event != null ? event.getUserId() : "null");
        if (event == null || event.getMobileNumber() == null) {
            log.error("Invalid DigiLocker event received: {}", event);
            return;
        }

        log.info("Consumed DigiLocker data for mobile: {}", event.getMobileNumber());

        Map<String, Object> startLog = new HashMap<>();
        startLog.put("mobileNumber", event.getMobileNumber());
        startLog.put("userId", event.getUserId());
        kafkaLogProducer.logInfo("KYC_PROCESSING_STARTED", startLog);

        // Fetch or create record (fixed lookup by userId or mobileNumber instead of findByEmail)
        KycRecord record = kycRecordRepository.findByUserId(event.getUserId())
                .or(() -> kycRecordRepository.findByMobileNumber(event.getMobileNumber()))
                .orElse(new KycRecord());

        // Full name construction (null-safe)
        String fullName =
                String.format(
                                "%s %s %s",
                                event.getFirstName() != null ? event.getFirstName() : "",
                                event.getMiddleName() != null ? event.getMiddleName() : "",
                                event.getLastName() != null ? event.getLastName() : "")
                        .replaceAll("\\s+", " ")
                        .trim();

        // Map fields
        record.setUserId(event.getUserId());
        record.setMobileNumber(event.getMobileNumber());
        record.setAadharNumber(event.getAadharNumber());
        record.setPanNumber(event.getPanNumber());
        record.setFirstName(event.getFirstName());
        record.setMiddleName(event.getMiddleName());
        record.setLastName(event.getLastName());
        record.setDob(event.getDob());
        record.setAddress(event.getAddress() != null ? event.getAddress().toString() : null);
        record.setStatus("VERIFIED");

        // Save to DB
        kycRecordRepository.save(record);
        log.info("KycRecord saved to DB for userId: {}", event.getUserId());

        // Synchronize KycEntity status to VERIFIED so bank access checks succeed
        if (event.getUserId() != null) {
            kycRepository.findById(event.getUserId()).ifPresentOrElse(kycEntity -> {
                kycEntity.setStatus("VERIFIED");
                if (kycEntity.getAadhaarNo() == null && event.getAadharNumber() != null) {
                    kycEntity.setAadhaarNo(event.getAadharNumber());
                }
                if (kycEntity.getPan() == null && event.getPanNumber() != null) {
                    kycEntity.setPan(event.getPanNumber());
                }
                kycRepository.save(kycEntity);
                log.info("KycEntity status updated to VERIFIED for userId: {}", event.getUserId());
            }, () -> {
                // If KycEntity does not exist yet, create it
                KycEntity newEntity = KycEntity.builder()
                        .userId(event.getUserId())
                        .aadhaarNo(event.getAadharNumber())
                        .pan(event.getPanNumber())
                        .status("VERIFIED")
                        .build();
                kycRepository.save(newEntity);
                log.info("Created new verified KycEntity for userId: {}", event.getUserId());
            });
        }

        // Produce success event
        KycVerificationSuccessEvent successEvent =
                KycVerificationSuccessEvent.builder()
                        .email(event.getEmail())
                        .mobileNumber(event.getMobileNumber())
                        .status("SUCCESS")
                        .fullName(fullName)
                        .build();

        log.info(
                "Sending KYC verification success event to Kafka topic {}: {}",
                SUCCESS_TOPIC,
                successEvent);

        kafkaTemplate.send(SUCCESS_TOPIC, event.getMobileNumber(), successEvent);

        Map<String, Object> successLog = new HashMap<>();
        successLog.put("mobileNumber", event.getMobileNumber());
        successLog.put("status", "SUCCESS");
        kafkaLogProducer.logInfo("KYC_PROCESSING_COMPLETED", successLog);
    }
}
