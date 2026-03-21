package rbi.notification.notification_systemds.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import rbi.notification.notification_systemds.redis.RedisNotificationSubscriber;
import rbi.notification.notification_systemds.service.impl.RedisSessionRegistry;

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
