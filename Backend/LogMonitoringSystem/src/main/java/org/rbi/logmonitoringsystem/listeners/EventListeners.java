package org.rbi.logmonitoringsystem.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.logmonitoringsystem.dtos.LogEventDTO;
import org.rbi.logmonitoringsystem.entity.LogEntity;
import org.rbi.logmonitoringsystem.repository.LogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class EventListeners {
    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"log-info", "log-warning", "log-error"},
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<LogEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            List<LogEntity> logs =
                    events.stream()
                            .map(
                                    event ->
                                            LogEntity.builder()
                                                    .serviceId(event.getServiceId())
                                                    .eventType(event.getEventType())
                                                    .level(event.getLevel())
                                                    .occurredAt(event.getOccurredAt())
                                                    .payload(
                                                            event.getPayload() != null
                                                                    ? toJson(event.getPayload())
                                                                    : null)
                                                    .build())
                            .toList();

            logRepository.saveAll(logs);
            log.debug("Successfully ingested {} audit log events into database", logs.size());
        } catch (Exception ex) {
            log.error("Failed to persist batch of {} log events: {}", events.size(), ex.getMessage(), ex);
            throw new RuntimeException("Error persisting audit logs batch", ex);
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize audit log payload: {}", e.getMessage());
            return "{\"error\": \"Failed to serialize payload\"}";
        }
    }
}
