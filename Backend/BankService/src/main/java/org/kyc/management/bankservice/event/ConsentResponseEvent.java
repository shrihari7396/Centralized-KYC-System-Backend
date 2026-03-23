package org.kyc.management.bankservice.event;

import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseEvent {
    private UUID consentId;
    private String userId;
    private UUID bankId;
    private ConsentStatus status;
    private List<DocumentType> requestedDocuments;
}
