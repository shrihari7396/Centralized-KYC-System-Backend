package com.rbi.apigateway.filters;

import com.rbi.apigateway.logging.KafkaLogProducer;
import com.rbi.apigateway.service.JwtService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtFilter implements GlobalFilter, Ordered {
    @Autowired private final JwtService jwtService;
    private final KafkaLogProducer kafkaLogProducer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String authheader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String path = exchange.getRequest().getURI().getPath();
        if (authheader == null || !authheader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        String token = authheader.substring(7);
        try {
            String role = jwtService.getUserRoles(token);
            //            temporary only for user
            if (path.startsWith("/users/") && !"USER".equals(role)) {
                return forbidden(exchange);
            }
            ServerHttpRequest mutatedrequest =
                    exchange.getRequest().mutate().header("X-User-Role", role).build();
            return chain.filter(exchange.mutate().request(mutatedrequest).build());
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        kafkaLogProducer.logWarning(
                "UNAUTHORIZED_ACCESS", Map.of("path", exchange.getRequest().getURI().getPath()));
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(401));
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        kafkaLogProducer.logWarning(
                "FORBIDDEN_ACCESS", Map.of("path", exchange.getRequest().getURI().getPath()));
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN));
        return exchange.getResponse().setComplete();
    }
}
