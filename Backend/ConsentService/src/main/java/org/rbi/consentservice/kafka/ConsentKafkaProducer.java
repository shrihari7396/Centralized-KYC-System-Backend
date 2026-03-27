package org.rbi.consentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.consentservice.config.KafkaTopicNameConstants;
import org.rbi.consentservice.event.ConsentResponseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendConsentResponseEvent(ConsentResponseEvent event) {
        log.info(
                "Sending ConsentResponseEvent to topic {} for consentId: {}",
                KafkaTopicNameConstants.CONSENT_RESPONSE_TOPIC,
                event.getConsentId());
        kafkaTemplate.send(KafkaTopicNameConstants.CONSENT_RESPONSE_TOPIC, event);
    }
}
