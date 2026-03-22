package com.rbi.consent.kafka;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.event.ConsentRequestEvent;
import com.rbi.consent.service.ConsentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaConsumer {

    private final ConsentService consentService;

    @KafkaListener(topics = "consent-request-topic", groupId = "consent-service-group")
    public void consumeConsentRequest(ConsentRequestEvent event) {
        log.info("Received Consent Request via Kafka for User: {}", event.getUserId());

        ConsentRequestDto requestDto =
                ConsentRequestDto.builder()
                        .userId(event.getUserId())
                        .bankId(UUID.fromString(event.getBankId()))
                        .requestedDocuments(event.getRequestedDocuments())
                        .consentId(event.getConsentId())
                        .build();

        consentService.requestConsent(requestDto);
    }
}
