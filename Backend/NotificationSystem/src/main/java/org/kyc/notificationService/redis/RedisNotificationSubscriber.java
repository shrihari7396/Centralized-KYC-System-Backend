package org.kyc.notificationService.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.kyc.notificationService.model.Notification;
import org.kyc.notificationService.service.NotificationService;
import org.kyc.notificationService.service.SessionRegistry;

@Slf4j
@Component
public class RedisNotificationSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final SessionRegistry sessionRegistry;

    public RedisNotificationSubscriber(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            NotificationService notificationService,
            SessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.sessionRegistry = sessionRegistry;
    }

    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            Notification notification = objectMapper.readValue(json, Notification.class);
            String userId = notification.getUserId();

            // 1. Check if this instance has active sessions for the user
            if (sessionRegistry.isUserOnline(userId)) {
                // 2. Atomic lock: claim the notification for sending
                if (notificationService.claimNotificationForSending(notification.getId())) {
                    try {
                        String destination = "/queue/notifications";
                        messagingTemplate.convertAndSendToUser(userId, destination, notification);
                        log.info(
                                "Successfully sent notification {} online to user {}",
                                notification.getId(),
                                userId);
                    } catch (Exception e) {
                        // 3. Failure Handling: Rollback to PENDING so it can be retried or
                        // delivered on reconnect
                        log.error(
                                "Failed to send notification {}. Rolling back to PENDING.",
                                notification.getId());
                        notificationService.rollbackToPending(notification.getId());
                        throw e;
                    }
                } else {
                    log.info(
                            "Notification {} already claimed by another node.",
                            notification.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process Redis notification message", e);
        }
    }
}
