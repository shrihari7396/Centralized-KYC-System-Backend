package org.kyc.digilockerService.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "digilocker")
public class DigilockerConfig {
    private String clientId;
    private String clientSecret;
    private String authUrl;
    private String tokenUrl;
    private String userUrl;
    private String redirectUri;
    private String apiBaseUrl;
}
