package org.rbi.kycmanagementservice.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KafkaLogProducerTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void logInfo_shouldSendLogEvent() {
        KafkaLogProducer producer = new KafkaLogProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "serviceId", "KYC-SERVICE");

        producer.logInfo("EVENT_TYPE", java.util.Map.of("key", "value"));

        ArgumentCaptor<LogEventDTO> captor = ArgumentCaptor.forClass(LogEventDTO.class);
        verify(kafkaTemplate).send(eq("log-info"), captor.capture());

        LogEventDTO event = captor.getValue();
        assertThat(event.getServiceId()).isEqualTo("KYC-SERVICE");
        assertThat(event.getEventType()).isEqualTo("EVENT_TYPE");
        assertThat(event.getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(event.getPayload()).containsEntry("key", "value");
        assertThat(event.getOccurredAt()).isNotNull();
    }
}
