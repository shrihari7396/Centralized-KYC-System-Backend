package com.rbi.apigateway.filters;

import com.rbi.apigateway.logging.KafkaLogProducer;
import com.rbi.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final KafkaLogProducer kafkaLogProducer;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;

    // Publicly accessible paths that do not require an authenticated JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/user/register",
            "/user/otp/request",
            "/user/otp/verify",
            "/bank/kyc/otp/request",
            "/bank/kyc/otp/verify",
            "/bank/kyc/banks",
            "/bank/kyc/register",
            "/api/digilocker/redirect",
            "/api/digilocker/otp/request",
            "/api/digilocker/otp/verify",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String authHeader = request.getHeaders().getFirst("Authorization");

        // Ensure Correlation ID is always present for distributed tracing
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;
        boolean isPublic = isPublicPath(path);

        // If public path and no auth header, forward with correlation header
        if (isPublic && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Correlation-Id", finalCorrelationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // If non-public path and auth header is missing or malformed -> reject with 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized access attempt to protected path: {} | CorrelationId: {}", path, finalCorrelationId);
            return unauthorized(exchange, "Authentication required. Missing Bearer token.", finalCorrelationId);
        }

        String token = authHeader.substring(7).trim();

        try {
            Claims claims = jwtService.extractAllClaims(token);

            if (!jwtService.validateToken(token)) {
                return unauthorized(exchange, "JWT token has expired or is invalid.", finalCorrelationId);
            }

            String jti = jwtService.getJti(claims);

            // Verify if token has been revoked via Redis blacklist
            if (jti != null && reactiveRedisTemplate != null) {
                return reactiveRedisTemplate.hasKey("blacklist:" + jti)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                log.warn("Access attempt with revoked token JTI: {}", jti);
                                return unauthorized(exchange, "Token has been revoked.", finalCorrelationId);
                            }
                            return forwardWithContextHeaders(exchange, chain, claims, finalCorrelationId, path);
                        })
                        .onErrorResume(e -> {
                            // If Redis check fails, log warning and proceed with cryptographic validation
                            log.warn("Redis blacklist check failed: {}", e.getMessage());
                            return forwardWithContextHeaders(exchange, chain, claims, finalCorrelationId, path);
                        });
            } else {
                return forwardWithContextHeaders(exchange, chain, claims, finalCorrelationId, path);
            }

        } catch (Exception e) {
            log.warn("JWT validation failed for path: {} | Error: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid JWT token signature or claims.", finalCorrelationId);
        }
    }

    private Mono<Void> forwardWithContextHeaders(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Claims claims,
            String correlationId,
            String path) {

        String role = jwtService.getUserRoles(claims);
        String userId = jwtService.getUserId(claims);
        String bankId = jwtService.getBankId(claims);

        // Path-based Role-Based Access Control
        if (path.startsWith("/user/") && !"USER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return forbidden(exchange, "Insufficient permissions for User service", correlationId);
        }

        if (path.startsWith("/bank/kyc/bypass") && !"ADMIN".equalsIgnoreCase(role)) {
            return forbidden(exchange, "Admin role required for compliance bypass", correlationId);
        }

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-Correlation-Id", correlationId)
                .header("X-User-Role", role)
                .header("X-User-Id", userId != null ? userId : "");

        if (bankId != null) {
            requestBuilder.header("X-Bank-Id", bankId);
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        for (String publicPrefix : PUBLIC_PATHS) {
            if (path.startsWith(publicPrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String detail, String correlationId) {
        kafkaLogProducer.logWarning(
                "UNAUTHORIZED_ACCESS",
                Map.of("path", exchange.getRequest().getURI().getPath(), "detail", detail, "correlationId", correlationId));

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(HttpStatus.UNAUTHORIZED.value()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"type\":\"https://errors.kyc-system.com/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"%s\",\"instance\":\"%s\",\"errorCode\":\"ERR_AUTH_REQUIRED\",\"correlationId\":\"%s\"}",
                detail, exchange.getRequest().getURI().getPath(), correlationId);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(jsonBody.getBytes())));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String detail, String correlationId) {
        kafkaLogProducer.logWarning(
                "FORBIDDEN_ACCESS",
                Map.of("path", exchange.getRequest().getURI().getPath(), "detail", detail, "correlationId", correlationId));

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(HttpStatus.FORBIDDEN.value()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"type\":\"https://errors.kyc-system.com/forbidden\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"%s\",\"instance\":\"%s\",\"errorCode\":\"ERR_ACCESS_DENIED\",\"correlationId\":\"%s\"}",
                detail, exchange.getRequest().getURI().getPath(), correlationId);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(jsonBody.getBytes())));
    }
}
