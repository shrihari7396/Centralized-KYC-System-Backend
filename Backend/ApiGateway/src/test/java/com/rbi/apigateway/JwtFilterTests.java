package com.rbi.apigateway;

import com.rbi.apigateway.filters.JwtFilter;
import com.rbi.apigateway.logging.KafkaLogProducer;
import com.rbi.apigateway.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JwtFilterTests {

    private JwtService jwtService;
    private KafkaLogProducer kafkaLogProducer;
    private ReactiveStringRedisTemplate redisTemplate;
    private JwtFilter jwtFilter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        kafkaLogProducer = Mockito.mock(KafkaLogProducer.class);
        redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        jwtFilter = new JwtFilter(jwtService, kafkaLogProducer, redisTemplate);
        chain = Mockito.mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void whenMissingAuthHeaderOnProtectedEndpoint_thenRejectWith401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/kyc/user123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtFilter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void whenAccessingPublicEndpointWithoutAuth_thenAllow() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/user/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtFilter.filter(exchange, chain))
                .verifyComplete();

        // Should forward to chain without rejecting
        assertEquals(null, exchange.getResponse().getStatusCode());
    }
}
