package org.rbi.consentservice.logging;

import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.consentservice.config.KafkaTopicNameConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.application.name}")
    private String serviceId;

    public void logInfo(String eventType, Map<String, Object> payload) {
        sendLog(KafkaTopicNameConstants.LOG_INFO_TOPIC, eventType, LogLevel.INFO, payload);
    }

    public void logWarning(String eventType, Map<String, Object> payload) {
        sendLog(KafkaTopicNameConstants.LOG_WARNING_TOPIC, eventType, LogLevel.WARNING, payload);
    }

    public void logError(String eventType, Map<String, Object> payload) {
        sendLog(KafkaTopicNameConstants.LOG_ERROR_TOPIC, eventType, LogLevel.ERROR, payload);
    }

    private void sendLog(
            String topic, String eventType, LogLevel level, Map<String, Object> payload) {
        LogEventDTO event =
                LogEventDTO.builder()
                        .serviceId(serviceId)
                        .eventType(eventType)
                        .level(level)
                        .occurredAt(Instant.now())
                        .payload(payload)
                        .build();
        kafkaTemplate
                .send(topic, event)
                .whenComplete(
                        (result, ex) -> {
                            if (ex != null) {
                                log.error(
                                        "Failed to publish Kafka log event {} to topic {}",
                                        eventType,
                                        topic,
                                        ex);
                                return;
                            }

                            log.debug(
                                    "Published Kafka log event {} to topic {} at offset {}",
                                    eventType,
                                    topic,
                                    result.getRecordMetadata().offset());
                        });
    }
}
