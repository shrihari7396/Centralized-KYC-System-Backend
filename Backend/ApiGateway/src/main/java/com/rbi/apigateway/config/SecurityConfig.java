package com.rbi.apigateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * CORS is configured centrally via the globalcors section in application.yml.
 * Do not add redundant CorsConfigurationSource beans to prevent duplicated headers.
 */
@Configuration
public class SecurityConfig {
    // WebFlux CORS handled via Spring Cloud Gateway configuration
}
