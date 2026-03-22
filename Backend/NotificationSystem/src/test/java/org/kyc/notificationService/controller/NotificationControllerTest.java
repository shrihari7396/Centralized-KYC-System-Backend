package org.kyc.notificationService.controller;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.notificationService.service.NotificationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationController notificationController;

    private Map<String, String> payload;

    @BeforeEach
    void setUp() {
        payload = new HashMap<>();
    }

    @Test
    void testAcknowledgeNotification_Success() {
        payload.put("notificationId", "notify-123");

        notificationController.acknowledgeNotification(payload);

        verify(notificationService).acknowledgeNotification("notify-123");
    }

    @Test
    void testAcknowledgeNotification_MissingId() {
        notificationController.acknowledgeNotification(payload);

        verifyNoInteractions(notificationService);
    }
}
