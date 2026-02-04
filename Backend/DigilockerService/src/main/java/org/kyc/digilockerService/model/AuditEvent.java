package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditEvent {
    private String eventType;
    private String userId;
    private String timestamp;
    private String source;
}
