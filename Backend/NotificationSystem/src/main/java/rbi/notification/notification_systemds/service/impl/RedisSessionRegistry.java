package rbi.notification.notification_systemds.service.impl;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import rbi.notification.notification_systemds.service.SessionRegistry;

@Service
public class RedisSessionRegistry implements SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionRegistry.class);
    private final StringRedisTemplate redisTemplate;

    @Value("${INSTANCE_ID:${random.uuid}}")
    private String instanceId;

    private static final String USER_PREFIX = "user:";
    private static final String INSTANCE_PREFIX = "instance:";

    // In-memory local mapping: userId -> Set of sessionIds on THIS instance
    private final ConcurrentHashMap<String, Set<String>> localSessions = new ConcurrentHashMap<>();

    public RedisSessionRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void registerSession(String userId, String sessionId) {
        // user:{userId} -> instanceId with TTL
        redisTemplate.opsForValue().set(USER_PREFIX + userId, instanceId, Duration.ofHours(1));

        // instance:{instanceId} -> Set of userIds (to know who is on this instance)
        redisTemplate.opsForSet().add(INSTANCE_PREFIX + instanceId, userId);

        // Local mapping for message delivery
        localSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.info("Registered session {} for user {} on instance {}", sessionId, userId, instanceId);
    }

    @Override
    public void unregisterSession(String userId, String sessionId) {
        if (userId != null) {
            Set<String> sessions = localSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    localSessions.remove(userId);
                    // Remove global mapping if user has no more sessions on this instance
                    redisTemplate.delete(USER_PREFIX + userId);
                    redisTemplate.opsForSet().remove(INSTANCE_PREFIX + instanceId, userId);
                    log.info(
                            "User {} is completely disconnected from instance {}",
                            userId,
                            instanceId);
                }
            }
        }
    }

    @Override
    public String getInstanceForUser(String userId) {
        return redisTemplate.opsForValue().get(USER_PREFIX + userId);
    }

    @Override
    public boolean isUserOnline(String userId) {
        return redisTemplate.hasKey(USER_PREFIX + userId);
    }

    @Override
    public Set<String> getLocalSessionsForUser(String userId) {
        return localSessions.getOrDefault(userId, Set.of());
    }
}
