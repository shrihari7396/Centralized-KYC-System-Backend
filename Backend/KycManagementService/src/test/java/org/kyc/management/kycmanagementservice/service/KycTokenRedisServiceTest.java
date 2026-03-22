package org.kyc.management.kycmanagementservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.management.kycmanagementservice.dto.KycTokenResponseDto;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KycTokenRedisServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private KycTokenRedisService service;

    @BeforeEach
    void setUp() {
        service = new KycTokenRedisService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void saveToken_shouldStoreJsonWithTtl() {
        KycTokenResponseDto token =
                KycTokenResponseDto.builder()
                        .tokenId("token-1")
                        .issuedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusDays(2).plusHours(1))
                        .status("ACTIVE")
                        .build();

        service.saveToken("user-1", token);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations)
                .set(
                        eq("KYC_TOKEN:user-1"),
                        any(String.class),
                        ttlCaptor.capture(),
                        eq(TimeUnit.DAYS));

        assertThat(ttlCaptor.getValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getToken_shouldDeserializeStoredJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        KycTokenResponseDto token =
                KycTokenResponseDto.builder()
                        .tokenId("token-1")
                        .issuedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .status("ACTIVE")
                        .build();
        String json = mapper.writeValueAsString(token);
        when(valueOperations.get("KYC_TOKEN:user-1")).thenReturn(json);

        Optional<KycTokenResponseDto> result = service.getToken("user-1");

        assertThat(result).isPresent();
        assertThat(result.get().getTokenId()).isEqualTo("token-1");
    }

    @Test
    void getToken_shouldReturnEmptyWhenMissing() {
        when(valueOperations.get("KYC_TOKEN:user-1")).thenReturn(null);

        Optional<KycTokenResponseDto> result = service.getToken("user-1");

        assertThat(result).isEmpty();
    }

    @Test
    void revokeToken_shouldMarkTokenAsRevoked() {
        KycTokenResponseDto token =
                KycTokenResponseDto.builder()
                        .tokenId("token-1")
                        .issuedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .status("ACTIVE")
                        .build();

        KycTokenRedisService spyService = spy(service);
        doReturn(Optional.of(token)).when(spyService).getToken("user-1");

        spyService.revokeToken("user-1");

        ArgumentCaptor<KycTokenResponseDto> captor =
                ArgumentCaptor.forClass(KycTokenResponseDto.class);
        verify(spyService).saveToken(eq("user-1"), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REVOKED");
    }
}
