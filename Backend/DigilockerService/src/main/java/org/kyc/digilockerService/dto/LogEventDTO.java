package org.kyc.digilockerService.dto;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
