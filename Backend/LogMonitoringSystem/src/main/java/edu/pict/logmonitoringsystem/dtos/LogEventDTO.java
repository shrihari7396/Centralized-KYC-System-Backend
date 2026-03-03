package edu.pict.logmonitoringsystem.dtos;

import edu.pict.logmonitoringsystem.entity.LogLevel;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class LogEventDTO {
    private String serviceId;
    private String eventType;
    private LogLevel level;
    private Instant occurredAt;
    private Map<String, Object> payload;
}
