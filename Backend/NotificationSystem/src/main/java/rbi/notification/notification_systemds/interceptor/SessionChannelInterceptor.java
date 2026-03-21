package rbi.notification.notification_systemds.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import rbi.notification.notification_systemds.service.SessionRegistry;

import java.security.Principal;

@Component
public class SessionChannelInterceptor implements ChannelInterceptor {

    private final SessionRegistry sessionRegistry;
    private final rbi.notification.notification_systemds.service.NotificationService notificationService;

    public SessionChannelInterceptor(SessionRegistry sessionRegistry, rbi.notification.notification_systemds.service.NotificationService notificationService) {
        this.sessionRegistry = sessionRegistry;
        this.notificationService = notificationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Assume userId is passed in a header like "X-User-Id" or "userId"
            String userId = accessor.getFirstNativeHeader("X-User-Id");
            if (userId == null) {
                userId = accessor.getFirstNativeHeader("userId");
            }

            if (userId != null) {
                final String finalUserId = userId;
                accessor.setUser(() -> finalUserId); // Set Principal
                
                String sessionId = accessor.getSessionId();
                sessionRegistry.registerSession(finalUserId, sessionId);
                
                // Fetch and send offline notifications
                notificationService.sendOfflineNotifications(finalUserId);
                
                System.out.println("Registered session: " + sessionId + " for user: " + finalUserId);
            } else {
                throw new IllegalArgumentException("Missing User Identity (X-User-Id)");
            }
        }

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            Principal user = accessor.getUser();
            if (sessionId != null && user != null) {
                sessionRegistry.unregisterSession(user.getName(), sessionId);
                System.out.println("Unregistered session: " + sessionId + " for user: " + user.getName());
            }
        }

        return message;
    }
}
