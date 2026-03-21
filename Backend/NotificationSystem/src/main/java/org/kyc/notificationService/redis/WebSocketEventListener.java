package org.kyc.notificationService.redis; // Or listener package

import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.kyc.notificationService.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    private final NotificationService notificationService;

    public WebSocketEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        // Check if subscribing to notifications queue
        if (destination != null && destination.endsWith("/queue/notifications")) {
            Principal principal = accessor.getUser();
            if (principal != null) {
                String targetId = principal.getName(); // This is the Global UUID
                log.info("Target subscribed: {}. Sending offline notifications.", targetId);

                // Trigger sending offline notifications
                notificationService.sendOfflineNotifications(targetId);
            }
        }
    }
}
