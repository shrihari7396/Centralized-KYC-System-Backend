package rbi.notification.notification_systemds.service;

import rbi.notification.notification_systemds.dto.NotificationEvent;
import rbi.notification.notification_systemds.model.Notification;
import rbi.notification.notification_systemds.model.NotificationStatus;

import java.util.List;

public interface NotificationService {
    
    void processNotification(NotificationEvent event);
    
    void sendOfflineNotifications(String userId);
    
    void acknowledgeNotification(String notificationId);
    
    void updateStatus(String notificationId, NotificationStatus status);

    boolean claimNotificationForSending(String notificationId);

    void rollbackToPending(String notificationId);

    List<Notification> getUnreadNotifications(String userId);
}
