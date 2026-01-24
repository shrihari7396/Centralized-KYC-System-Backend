package com.rbi.apigateway.filters;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter implements GlobalFilter {

    private final StringRedisTemplate redisTemplate;
    @Value("${rateLimitter.limit}")
    private String LIMIT;

    @Value("${rateLimitter.windowSize}")
    private String WINDOW;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String clientIp = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();

        String key = "rate_limit:" + clientIp;

        // 2. Increment request count
        Long count = redisTemplate.opsForValue().increment(key);

        // 3. Set expiry only for first request
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(Long.parseLong(WINDOW)));
        }

        // 4. Check limit
        if (count != null && count > Long.parseLong(LIMIT)) {
            exchange.getResponse()
                    .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
