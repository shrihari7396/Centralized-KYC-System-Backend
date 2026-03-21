package rbi.notification.notification_systemds.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rbi.notification.notification_systemds.dto.NotificationEvent;
import rbi.notification.notification_systemds.model.Notification;
import rbi.notification.notification_systemds.model.NotificationStatus;
import rbi.notification.notification_systemds.repository.NotificationRepository;
import rbi.notification.notification_systemds.service.NotificationService;
import rbi.notification.notification_systemds.service.SessionRegistry;
import rbi.notification.notification_systemds.utility.InstanceId;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final InstanceId instanceId;

    @Override
    @Transactional
    public void processNotification(NotificationEvent event) {
        // 1. Save in DB with status PENDING
        Notification notification =
                Notification.builder()
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
            if (targetInstanceId.equals(instanceId.getInstanceId())) {
                log.info(
                        "User {} is on the local instance. Bypassing Redis serialization.",
                        event.getUserId());
                String destination = "/queue/notifications";
                messagingTemplate.convertAndSendToUser(
                        event.getUserId(), destination, notification);
                notification.setStatus(NotificationStatus.DELIVERED);
                notificationRepository.save(notification);
            } else {
                try {
                    String json = objectMapper.writeValueAsString(notification);
                    redisTemplate.convertAndSend("notify:" + targetInstanceId, json);
                    log.info(
                            "Published to Redis for notification: {} to remote instance: {}",
                            event.getNotificationId(),
                            targetInstanceId);
                } catch (Exception e) {
                    log.error("Failed to publish message to Redis for remote instance delivery", e);
                }
            }
        } else {
            log.info(
                    "User {} is offline. Notification {} saved as PENDING.",
                    event.getUserId(),
                    event.getNotificationId());
        }
    }

    private void sendPendingNotification(String userId, List<Notification> notifications) {
        String destination =
                "/queue/notifications"; // STOMP /user/{userId}/queue/notifications translates to
        // this
        // Convert and send to user
        messagingTemplate.convertAndSendToUser(
                userId, // SimplePrincipal name (email)
                destination,
                notifications);

        // 3. Update status to DELIVERED
        notifications.forEach(notification -> notification.setStatus(NotificationStatus.DELIVERED));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void sendOfflineNotifications(String userId) {
        List<Notification> pendingNotifications =
                notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.PENDING);
        sendPendingNotification(userId, pendingNotifications);
    }

    @Override
    @Transactional
    public void updateStatus(String notificationId, NotificationStatus status) {
        notificationRepository
                .findById(notificationId)
                .ifPresent(
                        notification -> {
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
        notificationRepository
                .findById(notificationId)
                .ifPresent(
                        notification -> {
                            notification.setStatus(NotificationStatus.PENDING);
                            notificationRepository.save(notification);
                        });
    }

    @Override
    public void acknowledgeNotification(String notificationId) {
        updateStatus(
                notificationId,
                NotificationStatus.DELIVERED); // Not strictly needed without controller
    }

    @Override
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.PENDING);
    }
}
