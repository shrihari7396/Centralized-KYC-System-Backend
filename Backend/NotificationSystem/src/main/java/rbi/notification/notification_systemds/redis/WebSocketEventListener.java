package rbi.notification.notification_systemds.redis; // Or listener package

import java.security.Principal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import rbi.notification.notification_systemds.service.NotificationService;

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
                String userId = principal.getName(); // This is email
                System.out.println(
                        "User subscribed: " + userId + ". Sending offline notifications.");

                // Trigger sending offline notifications
                notificationService.sendOfflineNotifications(userId);
            }
        }
    }
}
