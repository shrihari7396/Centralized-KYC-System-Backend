package org.rbi.notificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rbi.notificationService.dto.NotificationEvent;
import org.rbi.notificationService.model.Notification;
import org.rbi.notificationService.model.NotificationStatus;
import org.rbi.notificationService.repository.NotificationRepository;
import org.rbi.notificationService.service.SessionRegistry;
import org.rbi.notificationService.service.impl.NotificationServiceImpl;
import org.rbi.notificationService.utility.InstanceId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private InstanceId instanceId;

    private NotificationServiceImpl notificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationService = new NotificationServiceImpl(
                notificationRepository, sessionRegistry, messagingTemplate, redisTemplate, objectMapper, instanceId);
    }

    @Test
    @DisplayName("processNotification routes directly to local user session if connected to local instance")
    void testProcessNotificationLocalUser() {
        when(instanceId.getInstanceId()).thenReturn("node-1");
        when(sessionRegistry.getInstanceForUser("target-user-123")).thenReturn("node-1");

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(UUID.randomUUID().toString())
                .targetId("target-user-123")
                .email("user@example.com")
                .message("KYC Verification Approved")
                .type("KYC_SUCCESS")
                .build();

        notificationService.processNotification(event);

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("target-user-123"), eq("/queue/notifications"), any(Notification.class));
    }

    @Test
    @DisplayName("processNotification publishes to Redis channel if user is on remote instance")
    void testProcessNotificationRemoteInstance() {
        when(instanceId.getInstanceId()).thenReturn("node-1");
        when(sessionRegistry.getInstanceForUser("target-user-456")).thenReturn("node-2");

        NotificationEvent event = NotificationEvent.builder()
                .notificationId("notif-456")
                .targetId("target-user-456")
                .email("user456@example.com")
                .message("Consent Requested")
                .type("CONSENT_REQUEST")
                .build();

        notificationService.processNotification(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(redisTemplate, times(1)).convertAndSend(eq("notify:node-2"), anyString());
    }

    @Test
    @DisplayName("acknowledgeNotification updates status to ACKNOWLEDGED")
    void testAcknowledgeNotification() {
        Notification notification = Notification.builder()
                .id("notif-999")
                .targetId("user-999")
                .status(NotificationStatus.DELIVERED)
                .build();

        when(notificationRepository.findById("notif-999")).thenReturn(Optional.of(notification));

        notificationService.acknowledgeNotification("notif-999");

        assertEquals(NotificationStatus.ACKNOWLEDGED, notification.getStatus());
        verify(notificationRepository, times(1)).save(notification);
    }
}
