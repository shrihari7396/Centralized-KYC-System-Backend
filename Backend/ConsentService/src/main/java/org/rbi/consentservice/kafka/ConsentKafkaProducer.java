package org.rbi.consentservice.kafka;

import org.rbi.consentservice.event.ConsentResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "consent-response-topic";

    public void sendConsentResponseEvent(ConsentResponseEvent event) {
        log.info(
                "Sending ConsentResponseEvent to topic {} for consentId: {}",
                TOPIC,
                event.getConsentId());
        kafkaTemplate.send(TOPIC, event);
    }
}
