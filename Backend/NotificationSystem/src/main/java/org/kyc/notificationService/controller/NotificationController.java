package org.kyc.notificationService.controller;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.kyc.notificationService.service.NotificationService;

@Slf4j
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
            log.info("Notification acknowledged: {}", notificationId);
        }
    }
}
