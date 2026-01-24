package com.rbi.apigateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private static final String LIMIT = "10";
    private static final String WINDOW = "60";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitFilter, "LIMIT", LIMIT);
        ReflectionTestUtils.setField(rateLimitFilter, "WINDOW", WINDOW);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void filter_Allowed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = rateLimitFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(valueOperations).increment("rate_limit:127.0.0.1");
        verify(redisTemplate).expire(eq("rate_limit:127.0.0.1"), any(Duration.class)); // verification for first request
        verify(chain).filter(exchange);
    }

    @Test
    void filter_RateLimited() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(valueOperations.increment(anyString())).thenReturn(11L); // Over limit

        // Act
        Mono<Void> result = rateLimitFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(valueOperations).increment("rate_limit:127.0.0.1");
        verify(chain, never()).filter(exchange);
        assert exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
    }
    
    @Test
    void filter_NotFirstRequest_NoExpireSet() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(valueOperations.increment(anyString())).thenReturn(5L); // Within limit, not first
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = rateLimitFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(valueOperations).increment("rate_limit:127.0.0.1");
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        verify(chain).filter(exchange);
    }
}
