package com.rbi.consent.kafka;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.enums.ConsentDocumentType;
import com.rbi.consent.event.ConsentRequestEvent;
import com.rbi.consent.service.ConsentService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

        List<ConsentDocumentType> documents =
                event.getRequestedDocuments().stream()
                        .map(
                                docStr -> {
                                    try {
                                        return ConsentDocumentType.valueOf(docStr);
                                    } catch (IllegalArgumentException e) {
                                        log.warn("Unknown document type requested: {}", docStr);
                                        return null; // Or handle dynamically
                                    }
                                })
                        .filter(doc -> doc != null)
                        .collect(Collectors.toList());

        ConsentRequestDto requestDto =
                ConsentRequestDto.builder()
                        .userId(event.getUserId())
                        .bankId(UUID.fromString(event.getBankId()))
                        .requestedDocuments(documents)
                        .build();

        consentService.requestConsent(requestDto);
    }
}
