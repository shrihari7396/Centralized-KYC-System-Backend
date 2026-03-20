package com.rbi.apigateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HmacConfig {

    @Value("${apigateway.security.secret-key}")
    private String secretKey;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Bean
    public SecretKeySpec hmacSecretKey() {
        return new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }
}
