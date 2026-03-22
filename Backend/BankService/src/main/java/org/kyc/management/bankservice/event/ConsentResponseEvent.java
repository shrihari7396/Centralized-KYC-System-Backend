package org.kyc.management.bankservice.event;

import lombok.*;

import java.util.List;
import java.util.UUID;

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
