package org.rbi.logmonitoringsystem.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.rbi.logmonitoringsystem.dtos.LogEventDTO;
import org.rbi.logmonitoringsystem.entity.LogEntity;
import org.rbi.logmonitoringsystem.repository.LogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EventListeners {
    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"log-info", "log-warning", "log-error"},
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<LogEventDTO> events) {

        try {
            // 2️⃣ Map DTO → Entity
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

            // 3️⃣ Persist to DB

            System.out.println(logs);
            logRepository.saveAll(logs);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize payload\"}";
        }
    }
}
