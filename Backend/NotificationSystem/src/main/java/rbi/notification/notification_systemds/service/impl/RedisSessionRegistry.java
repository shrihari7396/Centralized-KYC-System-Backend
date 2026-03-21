package rbi.notification.notification_systemds.service.impl;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import rbi.notification.notification_systemds.service.SessionRegistry;
import rbi.notification.notification_systemds.utility.InstanceId;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisSessionRegistry implements SessionRegistry {

    private final StringRedisTemplate redisTemplate;

    @Getter private final InstanceId instanceId;

    private static final String USER_PREFIX = "user:";
    private static final String INSTANCE_PREFIX = "instance:";

    // In-memory local mapping: userId -> Set of sessionIds on THIS instance
    private final ConcurrentHashMap<String, Set<String>> localSessions = new ConcurrentHashMap<>();

    @Override
    public void registerSession(String userId, String sessionId) {
        // user:{userId} -> instanceId with TTL
        redisTemplate
                .opsForValue()
                .set(USER_PREFIX + userId, instanceId.getInstanceId(), Duration.ofHours(1));

        // instance:{instanceId} -> Set of userIds (to know who is on this instance)
        redisTemplate.opsForSet().add(INSTANCE_PREFIX + instanceId.getInstanceId(), userId);

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
                    redisTemplate
                            .opsForSet()
                            .remove(INSTANCE_PREFIX + instanceId.getInstanceId(), userId);
                    log.info(
                            "User {} is completely disconnected from instance {}",
                            userId,
                            instanceId.getInstanceId());
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
