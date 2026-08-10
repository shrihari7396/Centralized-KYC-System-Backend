package org.rbi.notificationService.service.impl;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.notificationService.service.SessionRegistry;
import org.rbi.notificationService.utility.InstanceId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisSessionRegistry implements SessionRegistry {

    private final StringRedisTemplate redisTemplate;

    @Getter
    private final InstanceId instanceId;

    private static final String TARGET_PREFIX = "client:";
    private static final String INSTANCE_PREFIX = "instance:";

    // In-memory local mapping: targetId -> Set of sessionIds on THIS instance
    private final ConcurrentHashMap<String, Set<String>> localSessions = new ConcurrentHashMap<>();

    @Override
    public void registerSession(String targetId, String sessionId) {
        // client:{targetId} -> instanceId with TTL
        redisTemplate
                .opsForValue()
                .set(TARGET_PREFIX + targetId, instanceId.getInstanceId(), Duration.ofHours(1));

        // instance:{instanceId} -> Set of targetIds (to know who is on this instance)
        redisTemplate.opsForSet().add(INSTANCE_PREFIX + instanceId.getInstanceId(), targetId);

        // Local mapping for message delivery
        localSessions.computeIfAbsent(targetId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.info(
                "Registered session {} for target {} on instance {}",
                sessionId,
                targetId,
                instanceId);
    }

    @Override
    public void unregisterSession(String targetId, String sessionId) {
        if (targetId != null) {
            Set<String> sessions = localSessions.get(targetId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    localSessions.remove(targetId);
                    // Remove global mapping if user has no more sessions on this instance
                    redisTemplate.delete(TARGET_PREFIX + targetId);
                    redisTemplate
                            .opsForSet()
                            .remove(INSTANCE_PREFIX + instanceId.getInstanceId(), targetId);
                    log.info(
                            "Target {} is completely disconnected from instance {}",
                            targetId,
                            instanceId.getInstanceId());
                }
            }
        }
    }

    @Override
    public String getInstanceForUser(String targetId) {
        return redisTemplate.opsForValue().get(TARGET_PREFIX + targetId);
    }

    @Override
    public boolean isUserOnline(String targetId) {
        return redisTemplate.hasKey(TARGET_PREFIX + targetId);
    }

    @Override
    public Set<String> getLocalSessionsForUser(String targetId) {
        return localSessions.getOrDefault(targetId, Set.of());
    }
}
