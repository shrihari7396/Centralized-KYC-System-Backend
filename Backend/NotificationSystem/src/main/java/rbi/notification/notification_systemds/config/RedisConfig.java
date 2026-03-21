package rbi.notification.notification_systemds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import rbi.notification.notification_systemds.redis.RedisNotificationSubscriber;
import rbi.notification.notification_systemds.service.impl.RedisSessionRegistry;

@Configuration
public class RedisConfig {

    private final RedisSessionRegistry redisSessionRegistry;

    public RedisConfig(RedisSessionRegistry redisSessionRegistry) {
        this.redisSessionRegistry = redisSessionRegistry;
    }

    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("notify:" + redisSessionRegistry.getInstanceId());
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                        MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, topic());
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisNotificationSubscriber subscriber) {
        // "onMessage" is the method name in RedisNotificationSubscriber
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
