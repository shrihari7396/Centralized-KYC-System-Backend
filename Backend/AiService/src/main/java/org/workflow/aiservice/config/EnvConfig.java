package org.workflow.aiservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
    @PropertySource(value = "file:.env", ignoreResourceNotFound = true),
    @PropertySource(value = "file:./AiService/.env", ignoreResourceNotFound = true)
})
public class EnvConfig {}
