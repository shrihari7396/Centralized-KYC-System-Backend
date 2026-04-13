package org.rbi.kycmanagementservice.consumer;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.kycmanagementservice.configs.KafkaConstants;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.rbi.kycmanagementservice.event.DigiLockerDataFetchedEvent;
import org.rbi.kycmanagementservice.event.KycVerificationSuccessEvent;
import org.rbi.kycmanagementservice.logging.KafkaLogProducer;
import org.rbi.kycmanagementservice.repository.KycRecordRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycKafkaConsumer {

    private final KycRecordRepository kycRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaLogProducer kafkaLogProducer;

    private static final String FETCHED_TOPIC = KafkaConstants.DIGILOCKER_DATA_FETCHED_TOPIC;
    private static final String SUCCESS_TOPIC = KafkaConstants.KYC_VERIFICATION_SUCCESS;
    private static final String KAFKA_GROUP_ID = KafkaConstants.KAFKA_GROUP_ID;

    @KafkaListener(topics = FETCHED_TOPIC, groupId = KAFKA_GROUP_ID)
    public void consumeDigilockerData(DigiLockerDataFetchedEvent event) {
        //        DigiLockerDataFetchedEvent event =  modelMapper.map(event1,
        // DigiLockerDataFetchedEvent.class);
        log.info("Consume digilocker data received{}", event);
        // 🔴 Defensive null check (critical)
        if (event == null || event.getMobileNumber() == null) {
            log.error("Invalid DigiLocker event received: {}", event);
            return;
        }

        log.info("Consumed DigiLocker data for mobile: {}", event.getMobileNumber());

        // ✅ Safe logging map (replaced Map.of)
        Map<String, Object> startLog = new HashMap<>();
        startLog.put("mobileNumber", event.getMobileNumber());

        kafkaLogProducer.logInfo("KYC_PROCESSING_STARTED", startLog);

        // Fetch or create record
        KycRecord record =
                kycRecordRepository
                        .findByEmail(event.getMobileNumber())
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
        log.info("KycRecord saved to DB for mobile: {}", event.getMobileNumber());

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

        // ✅ Safe logging map (replaced Map.of)
        Map<String, Object> successLog = new HashMap<>();
        successLog.put("mobileNumber", event.getMobileNumber());
        successLog.put("status", "SUCCESS");

        kafkaLogProducer.logInfo("KYC_VERIFICATION_SUCCESS", successLog);
    }
}
