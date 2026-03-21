package rbi.notification.notification_systemds.service.impl;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rbi.notification.notification_systemds.dto.NotificationEvent;
import rbi.notification.notification_systemds.model.Notification;
import rbi.notification.notification_systemds.model.NotificationStatus;
import rbi.notification.notification_systemds.repository.NotificationRepository;
import rbi.notification.notification_systemds.service.NotificationService;
import rbi.notification.notification_systemds.service.SessionRegistry;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   SessionRegistry sessionRegistry,
                                   SimpMessagingTemplate messagingTemplate,
                                   org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                                   com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.sessionRegistry = sessionRegistry;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void processNotification(NotificationEvent event) {
        // 1. Save in DB with status PENDING
        Notification notification = Notification.builder()
                .id(event.getNotificationId())
                .userId(event.getUserId())
                .email(event.getEmail())
                .message(event.getMessage())
                .type(event.getType())
                .status(NotificationStatus.PENDING)
                .build();

        notificationRepository.save(notification);

        // 2. Look up the target instance of the user
        String targetInstanceId = sessionRegistry.getInstanceForUser(event.getUserId());
        if (targetInstanceId != null) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                redisTemplate.convertAndSend("notify:" + targetInstanceId, json);
                System.out.println("Published to Redis for notification: " + event.getNotificationId() + " to instance: " + targetInstanceId);
            } catch (Exception e) {
                e.printStackTrace(); // Handle appropriately in logs
            }
        } else {
            System.out.println("User " + event.getUserId() + " is offline. Notification " + event.getNotificationId() + " saved as PENDING.");
        }
    }

    private void sendNotificationToUser(Notification notification) {
        String destination = "/queue/notifications"; // STOMP /user/{userId}/queue/notifications translates to this
        // Convert and send to user
        messagingTemplate.convertAndSendToUser(
                notification.getUserId(), // SimplePrincipal name (email)
                destination,
                notification
        );

        // 3. Update status to DELIVERED
        notification.setStatus(NotificationStatus.DELIVERED);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendOfflineNotifications(String userId) {
        List<Notification> pendingNotifications = notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.PENDING);
        for (Notification notification : pendingNotifications) {
            sendNotificationToUser(notification);
        }
    }

    @Override
    @Transactional
    public void updateStatus(String notificationId, NotificationStatus status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notificationRepository.save(notification);
        });
    }

    @Override
    @Transactional
    public boolean claimNotificationForSending(String notificationId) {
        return notificationRepository.updateStatusToSentIfPending(notificationId) == 1;
    }

    @Override
    @Transactional
    public void rollbackToPending(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        });
    }

    @Override
    public void acknowledgeNotification(String notificationId) {
        updateStatus(notificationId, NotificationStatus.DELIVERED); // Not strictly needed without controller
    }

    @Override
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.PENDING);
    }
}
