package edu.pict.logmonitoringsystem.listeners;

import edu.pict.logmonitoringsystem.dtos.LogEventDTO;
import edu.pict.logmonitoringsystem.entity.LogEntity;
import edu.pict.logmonitoringsystem.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class EventListeners {
    private final LogRepository logRepository;

    @KafkaListener(
            topics = "application-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(List<LogEventDTO> events) {

        try {
            // 2️⃣ Map DTO → Entity
            List<LogEntity> logs = events.stream()
                    .map(event -> LogEntity.builder()
                            .serviceId(event.getServiceId())
                            .eventType(event.getEventType())
                            .level(event.getLevel())
                            .occurredAt(event.getOccurredAt())
//                            .payload(objectMapper.writeValueAsString(event.getPayload()))
                            .build()).toList();

            // 3️⃣ Persist to DB
            logRepository.saveAll(logs);
        } catch (Exception ex) {
            // ❌ DO NOT ACK → message will be retried
            // Later: send to DLT
            ex.printStackTrace();
        }
    }
}
