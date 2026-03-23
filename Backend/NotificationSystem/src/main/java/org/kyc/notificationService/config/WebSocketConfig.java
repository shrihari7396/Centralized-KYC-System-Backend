package org.kyc.notificationService.config;

import org.kyc.notificationService.interceptor.SessionChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SessionChannelInterceptor sessionChannelInterceptor;

    public WebSocketConfig(SessionChannelInterceptor sessionChannelInterceptor) {
        this.sessionChannelInterceptor = sessionChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix for server-side destinations, e.g., /app/ack
        config.setApplicationDestinationPrefixes("/app");
        // Destination prefixes for routing messages to clients
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for user-specific queues, e.g., /user/{userId}/queue/notifications
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure as needed for security
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register the Session Interceptor
        registration.interceptors(sessionChannelInterceptor);
    }
}
