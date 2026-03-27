package org.kyc.emailservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
    @PropertySource(value = "file:.env", ignoreResourceNotFound = true),
    @PropertySource(value = "file:./EmailService/.env", ignoreResourceNotFound = true)
})
public class EnvConfig {}
