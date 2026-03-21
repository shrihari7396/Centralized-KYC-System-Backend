package com.rbi.consent.config;

import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfig {

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
