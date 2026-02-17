package org.kyc.digilockerService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI digilockerMockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DigiLocker Mock Service API")
                        .description("Simulated Government Identity Verification APIs for Student Project")
                        .version("1.0"));
    }
}
