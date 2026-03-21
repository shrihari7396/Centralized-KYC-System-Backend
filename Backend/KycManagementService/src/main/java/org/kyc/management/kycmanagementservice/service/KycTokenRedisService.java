package org.kyc.management.kycmanagementservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.kyc.management.kycmanagementservice.dto.KycTokenResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class KycTokenRedisService {

    private static final String REDIS_TOKEN_PREFIX = "KYC_TOKEN:";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private final ObjectMapper objectMapper;

    public KycTokenRedisService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void saveToken(String userId, KycTokenResponseDto tokenDto) {
        try {
            String json = objectMapper.writeValueAsString(tokenDto);
            long daysToLive = Duration.between(LocalDateTime.now(), tokenDto.getExpiresAt()).toDays();
            if (daysToLive <= 0) daysToLive = 1;

            redisTemplate.opsForValue().set(
                REDIS_TOKEN_PREFIX + userId,
                json,
                daysToLive,
                TimeUnit.DAYS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing token to Redis", e);
        }
    }

    public Optional<KycTokenResponseDto> getToken(String userId) {
        String json = redisTemplate.opsForValue().get(REDIS_TOKEN_PREFIX + userId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, KycTokenResponseDto.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing token from Redis", e);
        }
    }

    public void revokeToken(String userId) {
        Optional<KycTokenResponseDto> existingToken = getToken(userId);
        if (existingToken.isPresent()) {
            KycTokenResponseDto token = existingToken.get();
            token.setStatus("REVOKED");
            saveToken(userId, token);
        }
    }
}
