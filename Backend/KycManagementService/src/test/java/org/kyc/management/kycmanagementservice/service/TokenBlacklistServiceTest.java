package org.kyc.management.kycmanagementservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    void isBlacklisted_shouldReturnFalseForBlankJti() {
        assertThat(service.isBlacklisted(null)).isFalse();
        assertThat(service.isBlacklisted(" ")).isFalse();
    }

    @Test
    void isBlacklisted_shouldReturnTrueWhenKeyPresent() {
        when(redisTemplate.hasKey("BLACKLISTED_JTI:jti-1")).thenReturn(true);

        assertThat(service.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    void blacklistToken_shouldWriteWithExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.blacklistToken("jti-1", 300L);

        verify(valueOperations)
                .set(eq("BLACKLISTED_JTI:jti-1"), eq("true"), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void blacklistToken_shouldIgnoreInvalidInputs() {
        service.blacklistToken(null, 100L);
        service.blacklistToken(" ", 100L);
        service.blacklistToken("jti-1", 0L);

        verify(valueOperations, org.mockito.Mockito.never())
                .set(any(String.class), any(String.class), any(Long.class), any(TimeUnit.class));
    }
}
