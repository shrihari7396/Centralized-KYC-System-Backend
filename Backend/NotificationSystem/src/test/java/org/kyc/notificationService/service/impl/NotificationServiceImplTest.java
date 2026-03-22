package org.kyc.notificationService.service.impl;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.notificationService.dto.NotificationEvent;
import org.kyc.notificationService.model.Notification;
import org.kyc.notificationService.model.NotificationStatus;
import org.kyc.notificationService.repository.NotificationRepository;
import org.kyc.notificationService.service.SessionRegistry;
import org.kyc.notificationService.utility.InstanceId;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private InstanceId instanceId;

    @InjectMocks private NotificationServiceImpl notificationService;

    private NotificationEvent event;
    private final String CURRENT_INSTANCE = "instance-1";

    @BeforeEach
    void setUp() {
        event = NotificationEvent.builder()
                .notificationId("notify-123")
                .targetId("user-456")
                .email("user@example.com")
                .message("Test message")
                .type("INFO")
                .build();
    }

    @Test
    void testProcessNotification_LocalInstance() {
        when(sessionRegistry.getInstanceForUser("user-456")).thenReturn(CURRENT_INSTANCE);
        when(instanceId.getInstanceId()).thenReturn(CURRENT_INSTANCE);

        notificationService.processNotification(event);

        verify(notificationRepository, times(2)).save(any(Notification.class)); // 1st PENDING, 2nd DELIVERED
        verify(messagingTemplate).convertAndSendToUser(eq("user-456"), eq("/queue/notifications"), any(Notification.class));
    }

    @Test
    void testProcessNotification_RemoteInstance() throws Exception {
        when(sessionRegistry.getInstanceForUser("user-456")).thenReturn("instance-2");
        when(instanceId.getInstanceId()).thenReturn(CURRENT_INSTANCE);
        when(objectMapper.writeValueAsString(any(Notification.class))).thenReturn("{}");

        notificationService.processNotification(event);

        verify(notificationRepository, times(1)).save(any(Notification.class)); // Only PENDING
        verify(redisTemplate).convertAndSend(eq("notify:instance-2"), anyString());
    }

    @Test
    void testProcessNotification_Offline() {
        when(sessionRegistry.getInstanceForUser("user-456")).thenReturn(null);

        notificationService.processNotification(event);

        verify(notificationRepository, times(1)).save(any(Notification.class)); // Only PENDING
        verifyNoInteractions(messagingTemplate);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testSendOfflineNotifications_Success() {
        Notification pending = Notification.builder().id("1").status(NotificationStatus.PENDING).build();
        when(notificationRepository.findByTargetIdAndStatus("user-456", NotificationStatus.PENDING))
                .thenReturn(Collections.singletonList(pending));

        notificationService.sendOfflineNotifications("user-456");

        verify(messagingTemplate).convertAndSendToUser(eq("user-456"), eq("/queue/notifications"), anyList());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void testUpdateStatus() {
        Notification notification = Notification.builder().id("1").status(NotificationStatus.PENDING).build();
        when(notificationRepository.findById("1")).thenReturn(Optional.of(notification));

        notificationService.updateStatus("1", NotificationStatus.DELIVERED);

        verify(notificationRepository).save(any(Notification.class));
        assertEquals(NotificationStatus.DELIVERED, notification.getStatus());
    }

    @Test
    void testClaimNotificationForSending_Success() {
        when(notificationRepository.updateStatusToSentIfPending("1")).thenReturn(1);

        boolean result = notificationService.claimNotificationForSending("1");

        assertTrue(result);
    }

    @Test
    void testRollbackToPending() {
        Notification notification = Notification.builder().id("1").status(NotificationStatus.SENT).build();
        when(notificationRepository.findById("1")).thenReturn(Optional.of(notification));

        notificationService.rollbackToPending("1");

        verify(notificationRepository).save(any(Notification.class));
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
    }
}
