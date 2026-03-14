package org.kyc.digilockerService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.event.DigiLockerDataFetchedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.kyc.digilockerService.util.RandomDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigilockerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "digilocker-data-fetched";

    public void processVerification(String mobileNumber) {
        log.info("Processing verification for mobile number: {}", mobileNumber);

        // Generate dummy data
        DigiLockerDataFetchedEvent event = DigiLockerDataFetchedEvent.builder()
                .mobileNumber(mobileNumber)
                .aadharNumber(UUID.randomUUID().toString())
                .panNumber(UUID.randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .middleName("Quincy")
                .dob(RandomDate.generateRandomDOB())
                .address(UUID.randomUUID().toString())
                .additionalDetails(new HashMap<>(Map.of("requestId", UUID.randomUUID().toString())))
                .build();

        log.info("Sending DigiLocker data fetched event to Kafka topic {}: {}", TOPIC, event);
        kafkaTemplate.send(TOPIC, mobileNumber, event);
    }
}
