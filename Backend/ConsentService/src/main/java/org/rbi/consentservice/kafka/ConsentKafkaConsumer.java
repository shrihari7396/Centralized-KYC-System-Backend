package org.rbi.consentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.consentservice.config.KafkaTopicNameConstants;
import org.rbi.consentservice.dto.ConsentRequestDto;
import org.rbi.consentservice.event.ConsentRequestEvent;
import org.rbi.consentservice.service.ConsentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaConsumer {

    private final ConsentService consentService;

    @KafkaListener(
            topics = KafkaTopicNameConstants.CONSENT_REQUEST_TOPIC,
            groupId = "consent-service-group")
    public void consumeConsentRequest(ConsentRequestEvent event) {
        log.info(
                "Received ConsentRequestEvent from topic {} for userId={}, bankId={}, consentId={}",
                KafkaTopicNameConstants.CONSENT_REQUEST_TOPIC,
                event.getUserId(),
                event.getBankId(),
                event.getConsentId());

        ConsentRequestDto requestDto =
                ConsentRequestDto.builder()
                        .userId(event.getUserId())
                        .bankId(event.getBankId())
                        .requestedDocuments(event.getRequestedDocuments())
                        .consentId(event.getConsentId())
                        .build();

        log.debug("Forwarding Kafka consent request to consentService.requestConsent()");
        consentService.requestConsent(requestDto);
        log.info("Consent request processing completed for consentId={}", event.getConsentId());
    }
}
