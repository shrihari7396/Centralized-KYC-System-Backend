package org.rbi.bankservice.event;

import java.util.List;
import java.util.UUID;
import lombok.*;
import org.rbi.bankservice.entity.DocumentType;

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
