package org.kyc.management.bankservice.logging;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogEventDTO {
  private String serviceId;
  private String eventType;
  private LogLevel level;
  private Instant occurredAt;
  private Map<String, Object> payload;
}
