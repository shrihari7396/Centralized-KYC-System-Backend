package org.rbi.notificationService.security;

import lombok.RequiredArgsConstructor;
import org.rbi.notificationService.exception.JwtValidationException;
import org.rbi.notificationService.service.SessionRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JWTService jwtService;
    private final SessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtService.validateToken(token)) {
                    String userId = jwtService.extractUserId(token);
                    accessor.setUser(new UserPrincipal(userId));

                    // Register session in Redis
                    String sessionId = accessor.getSessionId();
                    sessionRegistry.registerSession(userId, sessionId);
                } else {
                    throw new JwtValidationException("Invalid JWT Token");
                }
            } else {
                throw new JwtValidationException("Missing or invalid Authorization header");
            }
        }

        return message;
    }
}
