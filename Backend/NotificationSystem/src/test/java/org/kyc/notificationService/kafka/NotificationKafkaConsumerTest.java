package org.kyc.notificationService.kafka;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.notificationService.dto.NotificationEvent;
import org.kyc.notificationService.service.NotificationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationKafkaConsumerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationKafkaConsumer consumer;

    @Test
    void testConsume_Success() {
        NotificationEvent event =
                NotificationEvent.builder().notificationId("1").targetId("user-123").build();

        consumer.consume(event);

        verify(notificationService).processNotification(event);
    }
}
