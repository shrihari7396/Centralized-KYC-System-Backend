package org.rbi.logmonitoringsystem.dtos;

import org.rbi.logmonitoringsystem.entity.LogLevel;
import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class LogEventDTO {
    private String serviceId;
    private String eventType;
    private LogLevel level;
    private Instant occurredAt;
    private Map<String, Object> payload;
}
