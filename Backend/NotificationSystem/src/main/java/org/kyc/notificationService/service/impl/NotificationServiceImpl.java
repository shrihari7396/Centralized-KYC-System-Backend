package org.kyc.notificationService.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.notificationService.dto.NotificationEvent;
import org.kyc.notificationService.model.Notification;
import org.kyc.notificationService.model.NotificationStatus;
import org.kyc.notificationService.repository.NotificationRepository;
import org.kyc.notificationService.service.NotificationService;
import org.kyc.notificationService.service.SessionRegistry;
import org.kyc.notificationService.utility.InstanceId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                        .targetId(event.getTargetId())
                        .email(event.getEmail())
                        .message(event.getMessage())
                        .type(event.getType())
                        .status(NotificationStatus.PENDING)
                        .build();

        notificationRepository.save(notification);

        // 2. Look up the target instance of the target
        String targetInstanceId = sessionRegistry.getInstanceForUser(event.getTargetId());
        if (targetInstanceId != null) {
            if (targetInstanceId.equals(instanceId.getInstanceId())) {
                log.info(
                        "Target {} is on the local instance. Bypassing Redis serialization.",
                        event.getTargetId());
                String destination = "/queue/notifications";
                messagingTemplate.convertAndSendToUser(
                        event.getTargetId(), destination, notification);
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
                    "Target {} is offline. Notification {} saved as PENDING.",
                    event.getTargetId(),
                    event.getNotificationId());
        }
    }

    private void sendPendingNotification(String targetId, List<Notification> notifications) {
        String destination =
                "/queue/notifications"; // STOMP /user/{targetId}/queue/notifications translates to
        // this
        // Convert and send to user
        messagingTemplate.convertAndSendToUser(
                targetId, // SimplePrincipal name (Global ID)
                destination,
                notifications);

        // 3. Update status to DELIVERED
        notifications.forEach(notification -> notification.setStatus(NotificationStatus.DELIVERED));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void sendOfflineNotifications(String targetId) {
        List<Notification> pendingNotifications =
                notificationRepository.findByTargetIdAndStatus(
                        targetId, NotificationStatus.PENDING);
        sendPendingNotification(targetId, pendingNotifications);
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
    public List<Notification> getUnreadNotifications(String targetId) {
        return notificationRepository.findByTargetIdAndStatus(targetId, NotificationStatus.PENDING);
    }
}
