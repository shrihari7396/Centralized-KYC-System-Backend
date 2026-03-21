package rbi.notification.notification_systemds.interceptor;

import java.security.Principal;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import rbi.notification.notification_systemds.security.JWTService;
import rbi.notification.notification_systemds.security.UserPrincipal;
import rbi.notification.notification_systemds.service.NotificationService;
import rbi.notification.notification_systemds.service.SessionRegistry;

@Component
public class SessionChannelInterceptor implements ChannelInterceptor {

    private final SessionRegistry sessionRegistry;
    private final NotificationService notificationService;
    private final JWTService jwtService;

    public SessionChannelInterceptor(
            SessionRegistry sessionRegistry,
            @Lazy NotificationService notificationService,
            JWTService jwtService) {
        this.sessionRegistry = sessionRegistry;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) {
                throw new IllegalArgumentException("Invalid JWT Token");
            }

            String userId = jwtService.extractUserId(token);
            accessor.setUser(new UserPrincipal(userId));

            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                sessionRegistry.registerSession(userId, sessionId);
            }

            // Fetch and send offline notifications
            notificationService.sendOfflineNotifications(userId);

            System.out.println(
                    "Registered authenticated session: " + sessionId + " for user: " + userId);
        }

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            Principal user = accessor.getUser();
            if (sessionId != null && user != null) {
                sessionRegistry.unregisterSession(user.getName(), sessionId);
                System.out.println(
                        "Unregistered session: " + sessionId + " for user: " + user.getName());
            }
        }

        return message;
    }
}
