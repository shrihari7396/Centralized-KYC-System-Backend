package org.rbi.logmonitoringsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rbi.logmonitoringsystem.dtos.LogEventDTO;
import org.rbi.logmonitoringsystem.entity.LogLevel;
import org.rbi.logmonitoringsystem.listeners.EventListeners;
import org.rbi.logmonitoringsystem.repository.LogRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogMonitoringSystemTests {

    @Mock private LogRepository logRepository;
    private ObjectMapper objectMapper;
    private EventListeners eventListeners;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventListeners = new EventListeners(logRepository, objectMapper);
    }

    @Test
    @DisplayName("EventListeners batches and persists incoming log events to database")
    void testConsumeLogEventsBatch() {
        LogEventDTO event1 = LogEventDTO.builder()
                .serviceId("USER-SERVICE")
                .eventType("USER_LOGIN")
                .level(LogLevel.INFO)
                .occurredAt(Instant.now())
                .payload(Map.of("userId", "user-123"))
                .build();

        LogEventDTO event2 = LogEventDTO.builder()
                .serviceId("BANK-SERVICE")
                .eventType("KYC_REQUEST")
                .level(LogLevel.WARN)
                .occurredAt(Instant.now())
                .payload(Map.of("bankId", "bank-456"))
                .build();

        eventListeners.consume(List.of(event1, event2));

        verify(logRepository, times(1)).saveAll(argThat(logs -> {
            var list = (List<?>) logs;
            return list.size() == 2;
        }));
    }

    @Test
    @DisplayName("EventListeners ignores empty list safely without DB invocation")
    void testConsumeEmptyBatch() {
        eventListeners.consume(List.of());
        verify(logRepository, never()).saveAll(any());
    }
}
