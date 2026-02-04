package org.kyc.digilockerService.audit;

import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.model.AuditEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditPublisher {

    public void publishEvent(AuditEvent event) {
        // Stub for Kafka publishing
        log.info("AUDIT EVENT PUBLISHED: {}", event);
    }
}
