package org.rbi.notificationService.service;

import java.util.List;
import org.rbi.notificationService.dto.NotificationEvent;
import org.rbi.notificationService.model.Notification;
import org.rbi.notificationService.model.NotificationStatus;

public interface NotificationService {

    void processNotification(NotificationEvent event);

    void sendOfflineNotifications(String userId);

    void acknowledgeNotification(String notificationId);

    void updateStatus(String notificationId, NotificationStatus status);

    boolean claimNotificationForSending(String notificationId);

    void rollbackToPending(String notificationId);

    List<Notification> getUnreadNotifications(String userId);
}
