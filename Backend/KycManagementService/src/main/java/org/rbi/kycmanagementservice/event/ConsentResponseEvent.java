package org.rbi.kycmanagementservice.event;

import java.util.List;
import lombok.*;
import org.rbi.kycmanagementservice.dto.DocumentType;
import org.rbi.kycmanagementservice.enums.ConsentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseEvent {
    private String consentId;
    private String userId;
    private String bankId;
    private ConsentStatus status;
    private List<DocumentType> requestedDocuments;
}
