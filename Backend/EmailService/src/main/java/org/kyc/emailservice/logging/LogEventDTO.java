package org.kyc.emailservice.logging;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class LogEventDTO {
    private String serviceId;
    private String eventType;
    private LogLevel level;
    private Instant occurredAt;
    private Map<String, Object> payload;
}
