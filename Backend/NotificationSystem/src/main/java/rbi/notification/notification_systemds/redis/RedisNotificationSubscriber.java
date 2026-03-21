package rbi.notification.notification_systemds.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import rbi.notification.notification_systemds.model.Notification;
import rbi.notification.notification_systemds.service.NotificationService;
import rbi.notification.notification_systemds.service.SessionRegistry;

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
                        System.out.println(
                                "Successfully sent notification "
                                        + notification.getId()
                                        + " online to user "
                                        + userId);
                    } catch (Exception e) {
                        // 3. Failure Handling: Rollback to PENDING so it can be retried or
                        // delivered on reconnect
                        System.err.println(
                                "Failed to send notification "
                                        + notification.getId()
                                        + ". Rolling back to PENDING.");
                        notificationService.rollbackToPending(notification.getId());
                        throw e;
                    }
                } else {
                    System.out.println(
                            "Notification "
                                    + notification.getId()
                                    + " already claimed by another node.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle appropriately
        }
    }
}
