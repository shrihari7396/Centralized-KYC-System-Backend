package com.rbi.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventDTO {
    private String serviceId;
    private String eventType;
    private String level; // INFO, WARNING, ERROR
    private Instant occurredAt;
    private Map<String, Object> payload;
}
