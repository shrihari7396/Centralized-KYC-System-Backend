package org.kyc.management.kycmanagementservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.kycmanagementservice.configs.KafkaConstants;
import org.kyc.management.kycmanagementservice.entity.KycRecord;
import org.kyc.management.kycmanagementservice.event.DigiLockerDataFetchedEvent;
import org.kyc.management.kycmanagementservice.event.KycVerificationSuccessEvent;
import org.kyc.management.kycmanagementservice.logging.KafkaLogProducer;
import org.kyc.management.kycmanagementservice.repository.KycRecordRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycKafkaConsumer {

    private final KycRecordRepository kycRecordRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaLogProducer kafkaLogProducer;

    private static final String FETCHED_TOPIC = KafkaConstants.DIGILOCKER_DATA_FETCHED_TOPIC;
    private static final String SUCCESS_TOPIC = KafkaConstants.SUCCESS_TOPIC;
    private static final String KAFKA_GROUP_ID = KafkaConstants.KAFKA_GROUP_ID;

    @KafkaListener(topics = FETCHED_TOPIC, groupId = KAFKA_GROUP_ID)
    public void consumeDigilockerData(DigiLockerDataFetchedEvent event) {
        log.info("Consumed DigiLocker data for mobile: {}", event.getMobileNumber());
        kafkaLogProducer.logInfo(
                "KYC_PROCESSING_STARTED",
                Map.of("mobileNumber", event.getMobileNumber()));

        // Map and save to DB


        KycRecord record = KycRecord.builder()
                .dob()

                .build();

        kycRecordRepository.save(record);
        log.info("KycRecord saved to DB for mobile: {}", event.getMobileNumber());

        // Produce success event
        KycVerificationSuccessEvent successEvent =
                KycVerificationSuccessEvent.builder()
                        .mobileNumber(event.getMobileNumber())
                        .status("SUCCESS")
                        .fullName(fullName)
                        .build();

        log.info(
                "Sending KYC verification success event to Kafka topic {}: {}",
                SUCCESS_TOPIC,
                successEvent);
        kafkaTemplate.send(SUCCESS_TOPIC, event.getMobileNumber(), successEvent);

        kafkaLogProducer.logInfo(
                "KYC_VERIFICATION_SUCCESS",
                java.util.Map.of("mobileNumber", event.getMobileNumber(), "status", "SUCCESS"));
    }
}
