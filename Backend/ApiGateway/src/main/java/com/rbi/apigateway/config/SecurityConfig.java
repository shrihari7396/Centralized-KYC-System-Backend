package com.rbi.apigateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * CORS is configured centrally via the globalcors section in application.yml. Do NOT add a
 * CorsConfigurationSource bean here — it would cause the Access-Control-Allow-Origin header to be
 * set twice, which browsers reject.
 */
@Configuration
public class SecurityConfig {
    // No bean needed; CORS handled in application.yml globalcors
}

// PS C:\Users\Lenovo\Desktop\springbt\KYC\Centralized-KYC-System\Backend> div 4 DSE -> Shra K ->
// mok -> how's college going these days -> what tech learning currently -> -> are you interested in
// Tnp election
