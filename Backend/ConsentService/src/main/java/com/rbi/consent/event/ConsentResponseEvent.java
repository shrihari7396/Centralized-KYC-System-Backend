package com.rbi.consent.event;

import com.rbi.consent.enums.ConsentStatus;
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
