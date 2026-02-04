package edu.pict.logmonitoringsystem.listeners;

import edu.pict.logmonitoringsystem.config.AppConstants;
import edu.pict.logmonitoringsystem.dtos.LogEventDTO;
import edu.pict.logmonitoringsystem.entity.LogEntity;
import edu.pict.logmonitoringsystem.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Component
public class EventListeners {
    private final ObjectMapper objectMapper;
    private final LogRepository logRepository;

    @KafkaListener(
            topics = "application-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message, Acknowledgment ack) {

        try {
            // 1️⃣ Deserialize Kafka message
            LogEventDTO event =
                    objectMapper.readValue(message, LogEventDTO.class);

            // 2️⃣ Map DTO → Entity
            LogEntity logEntity = LogEntity.builder()
                    .serviceId(event.getServiceId())
                    .eventType(event.getEventType())
                    .level(event.getLevel())
                    .occurredAt(event.getOccurredAt())
                    .payload(objectMapper.writeValueAsString(event.getPayload()))
                    .build();

            // 3️⃣ Persist to DB
            logRepository.save(logEntity);

            // 4️⃣ Commit Kafka offset ONLY after DB success
            ack.acknowledge();

        } catch (Exception ex) {
            // ❌ DO NOT ACK → message will be retried
            // Later: send to DLT
            ex.printStackTrace();
        }
    }
}
