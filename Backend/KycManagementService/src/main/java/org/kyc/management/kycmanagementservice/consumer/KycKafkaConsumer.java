package org.kyc.management.kycmanagementservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.kycmanagementservice.entity.KycRecord;
import org.kyc.management.kycmanagementservice.event.DigiLockerDataFetchedEvent;
import org.kyc.management.kycmanagementservice.event.KycVerificationSuccessEvent;
import org.kyc.management.kycmanagementservice.logging.KafkaLogProducer;
import org.kyc.management.kycmanagementservice.repository.KycRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycKafkaConsumer {

    private final KycRecordRepository kycRecordRepository;

    @Autowired(required = false)
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaLogProducer kafkaLogProducer;

    private static final String FETCHED_TOPIC = "digilocker-data-fetched";
    private static final String SUCCESS_TOPIC = "kyc-verification-success";

    @KafkaListener(topics = FETCHED_TOPIC, groupId = "kyc-management-group")
    public void consumeDigilockerData(DigiLockerDataFetchedEvent event) {
        log.info("Consumed DigiLocker data for mobile: {}", event.getMobileNumber());
        kafkaLogProducer.logInfo(
                "KYC_PROCESSING_STARTED",
                java.util.Map.of("mobileNumber", event.getMobileNumber()));

        // Map and save to DB
        KycRecord record =
                kycRecordRepository
                        .findByMobileNumber(event.getMobileNumber())
                        .orElse(new KycRecord());

        String fullName =
                String.format(
                                "%s %s %s",
                                event.getFirstName(),
                                event.getMiddleName() != null ? event.getMiddleName() : "",
                                event.getLastName())
                        .replaceAll("\\s+", " ")
                        .trim();

        record.setMobileNumber(event.getMobileNumber());
        record.setAadharNumber(event.getAadharNumber());
        record.setPanNumber(event.getPanNumber());
        record.setFullName(fullName);
        record.setDob(event.getDob());
        record.setAddress(event.getAddress());
        record.setStatus("VERIFIED");

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
