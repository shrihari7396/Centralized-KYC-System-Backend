package org.kyc.notificationService.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.kyc.notificationService.redis.RedisNotificationSubscriber;
import org.kyc.notificationService.service.impl.RedisSessionRegistry;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisSessionRegistry redisSessionRegistry;

    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("notify:" + redisSessionRegistry.getInstanceId().getInstanceId());
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory, RedisNotificationSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, topic());
        return container;
    }
}
