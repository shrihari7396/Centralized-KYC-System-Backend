package com.rbi.consent.kafka;

import com.rbi.consent.event.ConsentApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "consent-approved-topic";

    public void sendConsentApprovedEvent(ConsentApprovedEvent event) {
        log.info("Sending ConsentApprovedEvent to topic {} for consentId: {}", TOPIC, event.getConsentId());
        kafkaTemplate.send(TOPIC, event);
    }
}
