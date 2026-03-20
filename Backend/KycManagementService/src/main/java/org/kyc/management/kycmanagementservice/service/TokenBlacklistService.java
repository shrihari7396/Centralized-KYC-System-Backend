package org.kyc.management.kycmanagementservice.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final String REDIS_JTI_PREFIX = "BLACKLISTED_JTI:";

    @Autowired private StringRedisTemplate redisTemplate;

    /** Checks if a token ID (JTI) is blacklisted. */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_JTI_PREFIX + jti));
    }

    /** Adds the JTI to the blacklist with the given expiration duration in seconds. */
    public void blacklistToken(String jti, long remainingExpirationSeconds) {
        if (jti != null && !jti.trim().isEmpty() && remainingExpirationSeconds > 0) {
            redisTemplate
                    .opsForValue()
                    .set(
                            REDIS_JTI_PREFIX + jti,
                            "true",
                            remainingExpirationSeconds,
                            TimeUnit.SECONDS);
        }
    }
}
