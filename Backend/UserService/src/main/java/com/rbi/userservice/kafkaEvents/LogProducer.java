package com.rbi.userservice.kafkaEvents;

import com.rbi.userservice.dto.LogEventDTO;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void info(String eventType, Map<String, Object> payload) {
    send("log-info", "INFO", eventType, payload);
  }

  public void warning(String eventType, Map<String, Object> payload) {
    send("log-warning", "WARNING", eventType, payload);
  }

  public void error(String eventType, Map<String, Object> payload) {
    send("log-error", "ERROR", eventType, payload);
  }

  private void send(String topic, String level, String eventType, Map<String, Object> payload) {
    LogEventDTO event =
        LogEventDTO.builder()
            .serviceId("UserService")
            .eventType(eventType)
            .level(level)
            .occurredAt(Instant.now())
            .payload(payload)
            .build();

    kafkaTemplate.send(topic, event);
  }
}
