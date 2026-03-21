package rbi.notification.notification_systemds.controller;

import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import rbi.notification.notification_systemds.service.NotificationService;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @MessageMapping("/ack")
    public void acknowledgeNotification(@Payload Map<String, String> payload) {
        String notificationId = payload.get("notificationId");
        if (notificationId != null) {
            notificationService.acknowledgeNotification(notificationId);
            System.out.println("Notification acknowledged: " + notificationId);
        }
    }
}
