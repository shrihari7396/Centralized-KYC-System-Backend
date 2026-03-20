package org.kyc.digilockerService.config;

import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfigs {

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
